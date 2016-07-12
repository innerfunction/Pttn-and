package com.innerfunction.pttn.app;

/**
 * An interface for modifying a view controller's standard behaviour.
 *
 * Created by juliangoacher on 12/07/16.
 */
public interface ViewControllerBehaviour {

    /** Called when the view controller resumes. */
    void onResume(ViewController view);

}
