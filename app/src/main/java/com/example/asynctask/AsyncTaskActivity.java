package com.example.asynctask;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AsyncTaskActivity extends Activity {

    private static final String TAG = "AsyncTaskActivity";
    private static final int P_BAR_MAX = 100;
    Button bStart;
    Button bStop;
    TextView textViewMessage;
    ProgressBar pBar;
    private UpdateTask myUpdateTask=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_async_task);

        bStart = (Button) findViewById(R.id.buttonStart);
        bStop = (Button) findViewById(R.id.buttonStop);
        textViewMessage = (TextView) findViewById(R.id.textView2);
        pBar = (ProgressBar) findViewById(R.id.progressBar1);

        //what is the max value
        pBar.setMax(P_BAR_MAX);

        //lets see if the device rotated and we need to regrab it
        myUpdateTask = (UpdateTask) getLastNonConfigurationInstance();

        //if a thread was retained then grab it
        if (myUpdateTask != null) {
            myUpdateTask.attach(this);
            pBar.setProgress(myUpdateTask.progress);
        }
    }

    /**
     * Called by the system, as part of destroying an
     * activity due to a configuration change, when it is known that a new
     * instance will immediately be created for the new configuration.  You
     * can return any object you like here, including the activity instance
     * itself, which can later be retrieved by calling
     * {@link #getLastNonConfigurationInstance()} in the new activity
     * instance.
     * <p/>
     * <em>If you are targeting {@link android.os.Build.VERSION_CODES#HONEYCOMB}
     * or later, consider instead using a {@link Fragment} with
     * {@link Fragment#setRetainInstance(boolean)
     * Fragment.setRetainInstance(boolean}.</em>
     * <p/>
     * <p>This function is called purely as an optimization, and you must
     * not rely on it being called.  When it is called, a number of guarantees
     * will be made to help optimize configuration switching:
     * <ul>
     * <li> The function will be called between {@link #onStop} and
     * {@link #onDestroy}.
     * <li> A new instance of the activity will <em>always</em> be immediately
     * created after this one's {@link #onDestroy()} is called.  In particular,
     * <em>no</em> messages will be dispatched during this time (when the returned
     * object does not have an activity to be associated with).
     * <li> The object you return here will <em>always</em> be available from
     * the {@link #getLastNonConfigurationInstance()} method of the following
     * activity instance as described there.
     * </ul>
     * <p/>
     * <p>These guarantees are designed so that an activity can use this API
     * to propagate extensive state from the old to new activity instance, from
     * loaded bitmaps, to network connections, to evenly actively running
     * threads.  Note that you should <em>not</em> propagate any data that
     * may change based on the configuration, including any data loaded from
     * resources such as strings, layouts, or drawables.
     * <p/>
     * <p>The guarantee of no message handling during the switch to the next
     * activity simplifies use with active objects.  For example if your retained
     * state is an {@link android.os.AsyncTask} you are guaranteed that its
     * call back functions (like {@link android.os.AsyncTask#onPostExecute}) will
     * not be called from the call here until you execute the next instance's
     * {@link #onCreate(android.os.Bundle)}.  (Note however that there is of course no such
     * guarantee for {@link android.os.AsyncTask#doInBackground} since that is
     * running in a separate thread.)
     *
     * @return Return any Object holding the desired state to propagate to the
     * next activity instance.
     * @deprecated Use the new {@link Fragment} API
     * {@link Fragment#setRetainInstance(boolean)} instead; this is also
     * available on older platforms through the Android compatibility package.
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        if (myUpdateTask != null) {
            Log.d(TAG, "onRetainNonConfigurationInstance");
            myUpdateTask.detach();
            return (myUpdateTask);
        } else
            return super.onRetainNonConfigurationInstance();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_async_task, menu);
        return true;
    }

    // make sure only one is enabled at a time===========
    private void setButtonState(boolean state) {
        bStart.setEnabled(!state);
        bStop.setEnabled(state);
    }

    // personal asynctask============

    // start thread
    public void doStart(View v) {
        setButtonState(true);
        textViewMessage.setText("Working...");

        //create and start thread
        myUpdateTask = new UpdateTask(this);
        myUpdateTask.execute();
    }

    //try to cancel thread
    public void doStop(View v) {
        setButtonState(false);
        textViewMessage.setText("Stopping...");
        pBar.setProgress(0);

        myUpdateTask.cancel(true);
    }

    //****************************************
    //notice I only define what will be returned to
    //onCanceled or onPostExecute
    //notice also that this is static, so it does not hold an implicit reference to enclosing
    //activity, rotate the phone and activity is GCed
    private static class UpdateTask extends AsyncTask<Void, Integer, String> {
        private static final String TAG = "UpdateTask";
        int progress = 1;
        private AsyncTaskActivity activity = null;

        // ===========================================
        // important do not hold a reference so garbage collector can grab old
        // defunct dying activity
        void detach() {
            activity = null;
            Log.d(TAG, "DETACHING");
        }

        public UpdateTask(AsyncTaskActivity activity) {
            attach(activity);
            Log.d(TAG, "          ATACHING");

        }

        // grab a reference to this activity, mindful of leaks
        void attach(AsyncTaskActivity activity) {
            if (activity == null)
                throw new IllegalArgumentException("Activity cannot be null");

            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            //do whatever setup you need here
        }

        /**
         * this is done in a new thread
         */
        @Override
        protected String doInBackground(Void... params) {
            for (int i = 1; i <= 10; i++) {
                //simulate some work sleep for .5 seconds
                SystemClock.sleep(500);

                //let main thread know we are busy
                //notice that we are autoboxing int to Integer
                publishProgress(i);

                //periodically check if the user canceled
                if (isCancelled())
                    return ("Canceled");
            }
            return "Finished";
        }

        /**
         * the following run on UI thread
         */
        @Override
        protected void onProgressUpdate(Integer... value) {
            super.onProgressUpdate(value);

            //indicate how far we have gone
            progress = value[0] * 10;
            activity.pBar.setProgress(progress);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            try {
                activity.textViewMessage.setText(result);
                activity.setButtonState(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onCancelled(String result) {
            super.onCancelled(result);

            try {
                activity.textViewMessage.setText(result);
                activity.setButtonState(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
