package com.techversat.lediview;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;

import com.techversat.ledimanager.Protocol;
import com.techversat.ledimanager.R;
import com.techversat.lediview.Dot;
import com.techversat.lediview.DotMatrix;
import com.techversat.lediview.DotView;


public class VirtualLEDIActivity extends Activity {
    /** Dot diameter */
    public static final int DOT_DIAMETER = 9;
    private Context context;

    
    /** Listen for taps. */
    private static final class TrackingTouchListener
        implements View.OnTouchListener
    {
        private final DotMatrix mDots;
        private List<Integer> tracks = new ArrayList<Integer>();
        private int pxpos = -1;
        private int pypos = -1;

        TrackingTouchListener(DotMatrix dots) { mDots = dots; }

        @Override
        public boolean onTouch(View v, MotionEvent evt) {
        	int idx;
        	int action = evt.getAction();   
        	switch (action & MotionEvent.ACTION_MASK) {
        	case MotionEvent.ACTION_DOWN:
        	case MotionEvent.ACTION_POINTER_DOWN:
        		idx = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)
        		>> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        		break;
        	case MotionEvent.ACTION_POINTER_UP:
        		idx = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)
        		>> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        		break;
        	case MotionEvent.ACTION_MOVE:
        		break;
        	default:
        		break;
        		// return false;
        	}
        	
        	final int hsize = evt.getHistorySize();
        	final int psize = evt.getPointerCount();

        	// process the historical coordinates
        	for (int h=0; h<hsize; h++) {
        		// idx = evt.findPointerIndex(i.intValue());
        		for (int p=0; p<psize; p++) {
        			float x = evt.getHistoricalX(p, h);
        			float y = evt.getHistoricalY(p, h);
        			int xpos = mDots.getXPos(x);
        			int ypos = mDots.getYPos(y);
        			if(xpos!=pxpos || ypos!=pypos) {
            			Log.i("DotMHist", "x="+x+" y="+y+" xpos="+xpos+" ypos="+ypos);
        				processDot(mDots, x, y,
        						evt.getHistoricalPressure(p, h),
        						evt.getHistoricalSize(p, h));
        				
        				pxpos=xpos;
        				pypos=ypos;
        			}
        		}
        	}
        	
        	// process the current coordinates
        	for(int p=0; p<psize; p++) {
    			float x = evt.getX(p);
    			float y = evt.getY(p);
    			int xpos = mDots.getXPos(x);
    			int ypos = mDots.getYPos(y);
    			if(xpos!=pxpos || ypos!=pypos) {
    				Log.i("DotMCurr", "x="+x+" y="+y+" xpos="+xpos+" ypos="+ypos);
    				processDot(mDots, x, y,
    						evt.getPressure(p),
    						evt.getSize(p));
    				
    				pxpos=xpos;
    				pypos=ypos;
    			}
        	}	
        	return true;
        }
        
        
        public boolean onTouch_old(View v, MotionEvent evt) {
            int n;
            int idx;
            int action = evt.getAction();
            pxpos = -1;
            pypos = -1;
            
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    idx = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                    // Integer.valueOf returns the instance of Integer object
                    tracks.add(Integer.valueOf(evt.getPointerId(idx)));
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    idx = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                    // removes all items with the given pointerId
                    while( tracks.remove(Integer.valueOf(evt.getPointerId(idx))) );
                    
                    break;

                case MotionEvent.ACTION_MOVE:
                	// Log.i("vLEDI", "hitting ACTION_MOVE");
                    n = evt.getHistorySize();
                    
                    for (Integer i: tracks) {
                        idx = evt.findPointerIndex(i.intValue());
                        for (int j = 0; j < n; j++) {
                        	float x = evt.getHistoricalX(idx, j);
                        	float y = evt.getHistoricalY(idx, j);
                        	int xpos = mDots.getXPos(x);
                        	int ypos = mDots.getYPos(y);
                        	if(xpos!=pxpos && ypos!=pypos) {	
                        		processDot(
                        			mDots,
                        			x,
                        			y,
                        			evt.getHistoricalPressure(idx, j),
                        			evt.getHistoricalSize(idx, j));
                        		pxpos=xpos;
                        		pypos=ypos;
                        	}
                        }
                    }
                    break;


                default:
                    return false;
            }

            for (Integer i: tracks) {
                idx = evt.findPointerIndex(i.intValue());
                float x = evt.getHistoricalX(idx, i);
            	float y = evt.getHistoricalY(idx, i);
            	int xpos = mDots.getXPos(x);
            	int ypos = mDots.getYPos(y);
            	
            	if(xpos!=pxpos && ypos!=pypos) {        
            		Log.i("DotM", "x="+x+" y="+y+" xpos="+xpos+" ypos="+ypos);
            		processDot(
            			mDots,
            			x,
            			y,
            			evt.getHistoricalPressure(idx, i),
            			evt.getHistoricalSize(idx, i));
            		pxpos=xpos;
            		pypos=ypos;
            	}
            }

            return true;
        }

        private void processDot(DotMatrix dots, float x, float y, float p, float s) {
            dots.findDot(
                x,
                y,
                Color.CYAN);
                // (int) ((p + 0.5) * (s + 0.5) * DOT_DIAMETER));
        }
    }

    /** Generate new dots, one per second. */
    private final class DotGenerator implements Runnable {
        final DotMatrix dots;
        final DotView view;
        final int color;

        private final Handler hdlr = new Handler();
        private final Runnable makeDots = new Runnable() {
            @Override public void run() { makeDot(dots, view, color); }
        };

        private volatile boolean done;

        DotGenerator(DotMatrix dots, DotView view, int color) {
            this.dots = dots;
            this.view = view;
            this.color = color;
        }

        public void done() { done = true; }

        @Override
        public void run() {
            while (!done) {
                hdlr.post(makeDots);
                try { Thread.sleep(5000); }
                catch (InterruptedException e) { }
            }
        }
    }

    private final Random rand = new Random();

    /** The application model */
    private DotMatrix dotModel;

    /** The application view */
    DotView dotView;

    /** The dot generator */
    DotGenerator dotGenerator;

    /** Called when the activity is first created. */
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);

        // install the view
        setContentView(R.layout.virtual_ledi);

        // find the dots view
        dotView = (DotView) findViewById(R.id.dots);
        /*
        int width = dotView.getRootView().getMeasuredWidth();
        int height = dotView.getRootView().getMeasuredHeight();
        int w1 = dotView.getWidth();
        int h1 = dotView.getHeight();
         */
        dotModel = new DotMatrix(32, 8);
        dotView.setDots(dotModel);

        dotView.setOnCreateContextMenuListener(this);
        dotView.setOnTouchListener(new TrackingTouchListener(dotModel));

        dotView.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (KeyEvent.ACTION_DOWN != event.getAction()) {
                    return false;
                }

                int color;
                switch (keyCode) {
                    case KeyEvent.KEYCODE_SPACE:
                        color = Color.MAGENTA;
                        break;
                    case KeyEvent.KEYCODE_ENTER:
                        color = Color.BLUE;
                        break;
                    default:
                        return false;
                }

                makeDot(dotModel, dotView, color);

                return true;
            } });


        /*
        dotView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus && (null != dotGenerator)) {
                    dotGenerator.done();
                    dotGenerator = null;
                }
                else if (hasFocus && (null == dotGenerator)) {
                    dotGenerator
                    = new DotGenerator(dotModel, dotView, Color.BLACK);
                    new Thread(dotGenerator).start();
                }
            } });
          */

        // wire up the controller
        ((Button) findViewById(R.id.button1)).setOnClickListener(
            new Button.OnClickListener() {
                @Override public void onClick(View v) {
                	Protocol.sendText(context, "w");
                	dotModel.clearDots();
                    // makeDot(dotModel, dotView, Color.RED);
                } });
        ((Button) findViewById(R.id.button2)).setOnClickListener(
            new Button.OnClickListener() {
                @Override public void onClick(View v) {
                	finish();
                    // go back to the main activity
                	// makeDot(dotModel, dotView, Color.GREEN);
                } });

        final EditText tb1 = (EditText) findViewById(R.id.text1);
        final EditText tb2 = (EditText) findViewById(R.id.text2);
        dotModel.setDotsChangeListener(new DotMatrix.DotsChangeListener() {
            @Override public void onDotsChange(DotMatrix dots) {
                Dot d = dots.getLastDot();
                // This code makes the UI unacceptably unresponsive.
                // ... investigating
                //tb1.setText((null == d) ? "" : String.valueOf(d.getX()));
                //tb2.setText((null == d) ? "" : String.valueOf(d.getY()));
                dotView.invalidate();
            } });
        
        // initialize the draw mode
        Protocol.sendText(context, ":d");
    }
    
    @Override public void onDestroy() {
    	Protocol.sendText(context, "q");
    	super.onDestroy();
    }

    /** Install an options menu. */
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.simple_menu, menu);
        return true;
    }

    /** Respond to an options menu selection. */
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear:
                dotModel.clearDots();
                Protocol.sendText(context, "w");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /** Install a context menu. */
    @Override public void onCreateContextMenu(
        ContextMenu menu,
        View v,
        ContextMenuInfo menuInfo)
    {
        menu.add(Menu.NONE, 1, Menu.NONE, "Clear")
            .setAlphabeticShortcut('x');
    }

    /** Respond to a context menu selection. */
    @Override public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                dotModel.clearDots();
                return true;
            default: ;
        }

        return false;
    }

    /**
     * @param dots the dots we're drawing
     * @param view the view in which we're drawing dots
     * @param color the color of the dot
     */
    void makeDot(DotMatrix dots, DotView view, int color) {
        int pad = (DOT_DIAMETER + 2) * 2;
        dots.findDot(
            DOT_DIAMETER + (rand.nextFloat() * (view.getWidth() - pad)),
            DOT_DIAMETER + (rand.nextFloat() * (view.getHeight() - pad)),
            color);
    }
}