// Copyright 2016 InnerFunction Ltd.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License
package com.innerfunction.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import android.content.Context;
import android.media.audiofx.EnvironmentalReverb;
import android.os.Environment;
import android.util.Log;

/**
 * Utility methods for reading and writing files and streams.
 * This class provides both a static and instance-based interface. The static API provides a variety
 * of useful methods for working with files on a standard file system. The instance API provides
 * alternative implementations of these methods that are able to work with the Android assets
 * filesystem in addition to standard files. These modified methods will recognize file paths
 * prefixed with /android_asset/ and forward the call to an appropriate method on the Assets class.
 * The instance methods will also automatically convert file URLs to path references.
 *
 * @author juliangoacher
 */
@SuppressWarnings("deprecation")
public class Files {

    static final String LogTag = "Files";

    private Assets assets;

    public Files(Context context) {
        this.assets = new Assets( context );
    }

    /**
     * Convert a file reference to a path.
     * @param ref   Either a file URL or a file path.
     * @return      A file path.
     */
    static final String fileRefToPath(final String ref) {
        if( ref.startsWith("file://") ) {
            return ref.substring( 7 );
        }
        return ref;
    }

    /**
     * Test whether a a file path references an Android asset.
     * Asset references begin with /android_asset/.
     * @param path  A file path.
     * @return Returns true if the path is an asset reference.
     */
    static final boolean isAssetPath(final String path) {
        return path.startsWith("/android_asset/");
    }

    /**
     * Convert a asset path reference to an asset name.
     * Strips the leading /android_asset/ from the path.
     * @param path
     * @return
     */
    static final String assetPathToName(final String path) {
        return path.substring( 15 );
    }

    /**
     * Convert a file reference to a URL.
     * Plain file paths have file:// prepended (with a leading / added if necessary); File URLs
     * are returned unchanged.
     */
    public static final String fileRefToURL(final String ref) {
        if( ref.startsWith("file://") ) {
            return ref;
        }
        String pathPrefix =  ref.charAt( 0 ) == '/' ? "" : "/";
        return String.format("file://%s%s", pathPrefix, ref );
    }

    /**
     * Test whether a file reference exists.
     * @param ref   A file URL or path.
     * @return Returns true if the file exists.
     */
    public boolean fileRefExists(String ref) {
        String path = fileRefToPath( ref );
        if( isAssetPath( path ) ) {
            return assets.assetExists( assetPathToName( path ) );
        }
        return Files.fileExists( path );
    }

    /**
     * Read a string from a file reference.
     * @param ref   A file URL or path.
     * @return Returns the text contents of the file, or null if it can't be read.
     */
    public String readStringFromRef(String ref) {
        String result = null;
        String path = fileRefToPath( ref );
        if( isAssetPath( path ) ) {
            InputStream in = null;
            try {
                in = assets.openInputStream( assetPathToName( path ) );
                result = Files.readString( in, ref );
            }
            catch(IOException e) {
                Log.e( LogTag, String.format("Reading stream %s", ref ), e );

            }
            finally {
                try {
                    in.close();
                }
                catch(Exception e) {}
            }
        }
        else {
            result = Files.readString( path );
        }
        return result;
    }

    /**
     * Move a file from one location to another.
     * @param sourceRef A file URL or path. If the referenced file is in the app's assets folder
     *                  then the file will be copied rather than moved.
     * @param targetRef The location to copy the file to.
     */
    public boolean mvFileRef(String sourceRef, String targetRef) {
        boolean result = false;
        String sourcePath = fileRefToPath( sourceRef );
        String targetPath = fileRefToPath( targetRef );
        if( isAssetPath( sourcePath ) ) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assets.openInputStream( assetPathToName( sourcePath ) );
                out = new FileOutputStream( targetPath );
                byte[] buffer = new byte[250 * 1024];
                int read;
                while( (read = in.read( buffer, 0, buffer.length )) > -1 ) {
                    out.write( buffer, 0, read );
                }
                out.flush();
                result = true;
            }
            catch(IOException e) {
                Log.e( LogTag, String.format("Copying stream %s -> %s", sourceRef, targetRef ), e );
            }
            finally {
                try {
                    in.close();
                }
                catch(Exception e) {}
                try {
                    out.close();
                }
                catch(Exception e) {}
            }
        }
        else {
            File source = new File( sourcePath );
            File target = new File( targetPath );
            result = Files.mv( source, target );
        }
        return result;
    }

    /** Test whether a file exists at the specified path. */
    public static boolean fileExists(String path) {
        return new File( path ).exists();
    }

    /** Test whether a directory exists at the specified path. */
    public static boolean dirExists(String path) {
        return new File( path ).isDirectory();
    }

    /** Read data from a file and return as a byte array. */
    public static byte[] readData(File file) throws FileNotFoundException{
        byte[] data = null;
        FileInputStream fin = null;
        try {
            fin = new FileInputStream( file );
            data = new byte[ (int)file.length() ];
            fin.read( data, 0, data.length );
        }
        catch(FileNotFoundException e){
            throw e;
        }
        catch(Exception e) {
            Log.e( LogTag, String.format("Reading file %s", file.getPath() ), e );
        }
        finally {
            try {
                fin.close();
            }
            catch(Exception e) {}
        }
        return data;
    }

    /**
     * Read data from an input stream and return as a byte array.
     * @param in    The input stream to read from.
     * @param name  A name (e.g. a filename) associated with the stream; used for logging.
     * @return A byte array containing the data.
     */
    public static byte[] readData(InputStream in, String name) {
        ByteArrayOutputStream result = new ByteArrayOutputStream( 16384 );
        try {
            byte[] buffer = new byte[16384];
            int read;
            while( true ) {
                read = in.read( buffer, 0, buffer.length );
                if( read > 0 ) {
                    result.write( buffer, 0, read );
                }
                else break; // End of stream.
            }
        }
        catch(Exception e) {
            Log.e( LogTag, String.format("Reading stream %s", name ), e );
        }
        finally {
            try {
                in.close();
            }
            catch(Exception e) {}
        }
        return result.toByteArray();
    }


    /**
     * Read data from a file and return as a string.
     * @param path      The path to the file to read from.
     * @return A string containing the file's data.
     */
    public static String readString(String path) {
        return readString( new File( path ) );
    }

    /**
     * Read data from a file and return as a string.
     * @param file      The file to read from.
     * @return A string containing the file's data.
     */
    public static String readString(File file) {
        String str = null;
        try {
            str = new String( Files.readData( file ), "UTF-8");
        }
        catch(FileNotFoundException e) {
            Log.e( LogTag, String.format("File not found %s", file.getAbsolutePath()));
        }
        catch(UnsupportedEncodingException e) {
            Log.e( LogTag, "UTF-8 decoding error");
        }
        return str;
    }

    /**
     * Read data from an input stream and return as a string.
     * @param in    The input stream to read from.
     * @param name  A name (e.g. a filename) associated with the stream; used for logging.
     * @return A string containing the data.
     */
    public static String readString(InputStream in, String name) {
        String str = null;
        try {
            str = new String( Files.readData( in, name ), "UTF-8");
        }
        catch(UnsupportedEncodingException e) {
            Log.e( LogTag, "UTF-8 decoding error");
        }
        return str;
    }

    /**
     * Read a JSON file.
     * @param file      The file to read from.
     * @return An object representing the parsed file contents, or null if the file isn't found or
     * contains no data.
     */
    public static Object readJSON(File file) {
        String json = readString( file );
        return json != null ? JSONValue.parse( json ) : null;
    }

    /**
     * Read JSON from an input stream.
     * @param in        The stream to read from.
     * @param name      A name (e.g. filename) associated with the stream; used for logging.
     * @return An object representing the parsed stream contents, or null if the input stream
     * contains no data.
     * @throws ParseException   If the stream doesn't contain valid JSON.
     */
    public static Object readJSON(InputStream in, String name) {
        String json = readString( in, name );
        return json != null ? JSONValue.parse( json ) : null;
    }

    /**
     * Write data from an input stream to a file.
     * Overwrites any data already in the file.
     * @param file  The file to write to.
     * @param in    An input stream containing the data to write.
     * @return Returns true if the data was successfully written.
     */
    public static boolean writeData(File file, InputStream in) {
        return Files.writeData( file, in, false );
    }

    /**
     * Write data from an input stream to a file.
     * @param file      The file to write to.
     * @param in        An input stream containing the data to write.
     * @param append    If true then data is appended to the file; otherwise the file is
     *                  overwritten.
     * @return Returns true if the data was successfully written.
     */
    public static boolean writeData(File file, InputStream in, boolean append) {
        boolean ok = true;
        FileOutputStream fout = null;
        try {
            byte[] buffer = new byte[16384];
            fout = new FileOutputStream( file, append );
            int length;
            while( (length = in.read( buffer )) > 0 ) {
                fout.write( buffer, 0, length );
            }
        }
        catch(Exception e) {
            Log.e(LogTag, String.format("Writing %s", file ), e );
        }
        finally {
            try {
                fout.close();
            }
            catch(Exception e) {}
        }
        return ok;
    }

    /**
     * Write a string to a file.
     * Uses the system default string encoding. Overwrites any data already in the file.
     * @param file  The file to write to.
     * @param s     The string to write.
     * @return Returns true if the string was successfully written to the file.
     */
    public static boolean writeString(File file, String s) {
        return Files.writeData( file, new StringBufferInputStream( s ) );
    }

    /**
     * Write JSON data to a file.
     * Uses the system default string encoding. Overwrites any data already in the file.
     * @param file  The file to write to.
     * @param data  The data to write. Must be JSON encodable.
     * @return Returns true if the data was successfully encoded and written to the file.
     */
    public static boolean writeJSON(File file, Object data) {
        String json = JSONValue.toJSONString( data );
        return writeString( file, json );
    }

    /**
     * Unzip a zip archive.
     * @param zipFile   The file containing the zip archive.
     * @param targetDir The directory to write the archive's contents to.
     * @return Returns An array containing the full path of each unzipped file, or null if the file
     *         couldn't be unzipped.
     */
    public static String[] unzip(File zipFile, File targetDir) {
        FileInputStream in = null;
        try {
            in = new FileInputStream( zipFile );
            return unzip( in, targetDir );
        }
        catch(Exception e) {
            Log.e(LogTag, String.format("Unzipping %s", zipFile ), e );
        }
        return null;
    }

    /**
     * Unzip an input stream containing a zip archive.
     * @param in        The stream containing the zip archive.
     * @param targetDir The directory to write the archive's contents to.
     * @return Returns An array containing the full path of each unzipped file, or null if the file
     *         couldn't be unzipped.
     */
    public static String[] unzip(InputStream in, File targetDir) {
        String[] result = null;
        List<String> files = new ArrayList<>();
        ZipInputStream zin = null;
        try {
            zin = new ZipInputStream( in );
            ZipEntry entry;
            // Process each zip file entry.
            while( (entry = zin.getNextEntry()) != null ) {
                String fileName = entry.getName();
                File entryFile = new File( targetDir, fileName );
                if( entry.isDirectory() ) {
                    if( !entryFile.isDirectory() ) {
                        entryFile.mkdirs();
                    }
                }
                else {
                    File parentDir = entryFile.getParentFile();
                    if( !parentDir.isDirectory() ) {
                        parentDir.mkdirs();
                    }
                    Files.writeData( entryFile, zin );
                }
                zin.closeEntry();
                files.add( entryFile.getAbsolutePath() );
            }
            // Generate list of unpacked file paths.
            result = files.toArray( new String[files.size()] );
        }
        catch(Exception e) {
            Log.e(LogTag, "Unzipping input stream");
        }
        finally {
            try {
                zin.close();
            }
            catch(Exception e) {}
        }
        return result;
    }

    /** Get the directory to use for content caching. */
    public static File getCacheDir(Context context) {
        File cacheDir = context.getExternalCacheDir();
        if( cacheDir == null ) {
            cacheDir = context.getCacheDir();
        }
        return cacheDir;
    }

    /** Get the directory to use for app storage. */
    public static File getStorageDir(Context context) {
        File storageDir = context.getExternalFilesDir( null );
        if( storageDir == null ) {
            storageDir = context.getFilesDir();
        }
        return storageDir;
    }

    /**
     * Remove a directory from the filesystem.
     * Delete's the directory and all of its contents. The function works by first moving (i.e.
     * renaming) the directory to a temporary location, and then deleteing it. This is done because
     * otherwise problems can occur if the directory location is to be reused immediately
     * afterwards.
     * @param dir       The directory to delete.
     * @param context   The app context.
     * @return Returns true if the directory was successfully deleted.
     */
    public static boolean removeDir(File dir, Context context) {
        boolean ok = false;
        File cacheDir = Files.getCacheDir( context );
        if( dir.exists() && dir.toString().startsWith( cacheDir.toString() ) ) {
            Runtime rt = Runtime.getRuntime();
            try {
                // Delete the directory by first moving to a temporary location, then deleting.
                // This is because deleting in place will cause problems if the location is to be written
                // to immediately after.
                File temp = new File( dir.getParentFile(), String.format("fileio-%d-rm", System.currentTimeMillis() ) );
                dir.renameTo( temp );
                String cmd = String.format("rm -Rf %s", temp );
                @SuppressWarnings("unused")
                Process p = rt.exec( cmd );
                //p.waitFor(); Uncomment if should wait for rm to complete before continuing (i.e. synchronous behaviour)
                ok = true;
            }
            catch(Exception e) {
                Log.e(LogTag, String.format("Removing directory %s", dir ), e );
            }
        }
        return ok;
    }

    /**
     * Move a file or files from a source location to a target location.
     * Handles situations where the source and destination files are on different disk partitions.
     * Also creates parent directories for the target file, if necessary.
     * @param from The location to move from. May be a single file or a directory. If a directory
     *             then all of its file contents are also moved.
     * @param to   The location to move to. May be specified as a file or directory. Overwrites
     *             any file already at that location.
     * @return true if all files were moved.
     */
    public static boolean mv(File from, File to) {
        boolean ok = true;
        if( from.exists() ) {
            if( from.isDirectory() ) {
                if( ensureDirectoryExists( to, true ) ) {
                    // Iterate over all files in the from directory and move them to the target.
                    String[] filenames = from.list();
                    for( String filename : filenames ) {
                        File file = new File( from, filename );
                        ok &= mv( file, to );
                        if( !ok ) {
                            break;
                        }
                    }
                }
                else ok = false; // Couldn't create target directory.
            }
            else {
                // Check whether 'to' is a file or directory:
                // * If a directory, then derive the target file by appending the from filename to
                //   the target path;
                // * If a file then get the target directory as the file's parent.
                File toDir;
                if( to.isDirectory() ) {
                    toDir = to;
                    to = new File( toDir, from.getName() );
                }
                else {
                    toDir = to.getParentFile();
                }
                if( ensureDirectoryExists( toDir, true ) ) {
                    ok = from.renameTo( to );
                    // File.renameTo() can fail if the 'from' and 'to' files are on different disk
                    // partitions (although note that there are many other reasons why it could fail
                    // also - e.g. file permissions). A better implementation might check first if
                    // the two files are on the same partition; however, unfortunately - but not
                    // surprisingly - android provides no methods for doing this simply.
                    if( !ok ) {
                        // Try copying contents from source file to the destination, and then
                        // deleting the source.
                        try {
                            FileInputStream in = new FileInputStream( from );
                            ok = writeData( to, in );
                            if( ok ) {
                                from.delete();
                            }
                        }
                        catch(IOException e) {}
                    }
                }
                else ok = false; // Couldn't create target directory.
            }
        }
        return ok;
    }

    /**
     * Ensure a directory exists at the path specified by a file.
     * @param file  A file identifying where the directory should be.
     * @param deleteExistingFile If true, and if a file already exists at the required path which
     *                           isn't a directory, then delete that file before creating a new
     *                           directory.
     * @return true if a directory exists at the required path.
     */
    public static boolean ensureDirectoryExists(File file, boolean deleteExistingFile) {
        if( !file.exists() ) {
            // If no file exists at the specified path then create a new directory and all
            // required parent directories.
            return file.mkdirs();
        }
        boolean isDir = file.isDirectory();
        if( !isDir && deleteExistingFile ) {
            // If a non-directory file already exists at the required path then delete it before
            // creating a new directory.
            if( file.delete() ) {
                return file.mkdir();
            }
            return false; // File couldn't be deleted.
        }
        return isDir; // Return whether the pre-existing file is a directory.
    }

    /**
     * Remove a file from the file system.
     * Handles single files and directories. For directories, will recursively delete all files
     * and directories below the directory before deleting the file.
     * @param file
     * @return true if the file and all its contents were successfully deleted.
     */
    public static boolean rm(File file) {
        boolean ok = true;
        if( file.isDirectory() ) {
            // If the file is a directory then recursively delete all of its contents.
            String[] filenames = file.list();
            for( String filename : filenames ) {
                ok &= rm( new File( file, filename ) );
                if( !ok ) {
                    break;
                }
            }
        }
        if( ok ) {
            ok &= file.delete();
        }
        return ok;
    }

}
