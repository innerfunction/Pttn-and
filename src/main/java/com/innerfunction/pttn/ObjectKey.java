package com.innerfunction.pttn;

/**
 * A class which allows using arbitrary objects as keys in hash-based maps.
 * Objects belonging to classes which provide proper implementations of the hashCode() and equals()
 * methods may have different instances mapped to the same hash map entry because they are
 * semantically equal (i.e. o1 != o2 but o1.equals( o2 ) == true). The Container code needs to
 * reliably perform map lookups using arbitrary object instances, and so uses this class to
 * construct object keys. This class only assumes that any object instance consistently returns the
 * same result when hashCode() is called. This class uses object identify as its equality test,
 * rather than delegating to the object's equals() method.
 * Created by juliangoacher on 29/03/16.
 */
public class ObjectKey {

    /** The object whose key this represents. */
    private Object object;

    public ObjectKey(Object object) {
        this.object = object;
    }

    /** Return the hash code of the wrapped object. */
    public int hashCode() {
        return object.hashCode();
    }

    /** Test that the argument is the same instance as the wrapped object. */
    public boolean equals(Object obj) {
        return this.object == obj;
    }
}
