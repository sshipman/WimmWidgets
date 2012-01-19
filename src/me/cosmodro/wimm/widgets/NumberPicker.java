/*
 * Based on NumberPicker from Jeffrey F. Cole.
 * Added Scroll/Fling handling and button label configuration
 * 
 * Copyright (c) 2010, Jeffrey F. Cole
 * Copyright (c) 2012, Steve Shipman
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * 	Redistributions of source code must retain the above copyright notice, this
 * 	list of conditions and the following disclaimer.
 * 
 * 	Redistributions in binary form must reproduce the above copyright notice, 
 * 	this list of conditions and the following disclaimer in the documentation 
 * 	and/or other materials provided with the distribution.
 * 
 * 	Neither the name of the technologichron.net nor the names of its contributors 
 * 	may be used to endorse or promote products derived from this software 
 * 	without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
package me.cosmodro.wimm.widgets;

import android.content.Context;
import android.os.Handler;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import com.wimm.framework.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A simple layout group that provides a numeric text area with two buttons to
 * increment or decrement the value in the text area. Holding either button will
 * auto increment the value up or down appropriately.
 * 
 * @author Steve Shipman, Jeffrey F. Cole
 * 
 */
public class NumberPicker extends LinearLayout {
	private static String TAG = "NumberPicker";

	private final long REPEAT_DELAY = 50;

	private final int ELEMENT_HEIGHT = 50;
	private final int ELEMENT_WIDTH = ELEMENT_HEIGHT; // you're all squares, yo

	private int minimum = 0;
	private int maximum = 999;

	public Integer value;

	Button decrement;
	Button increment;
	public TextView valueText;

	private Handler repeatUpdateHandler = new Handler();

	private boolean autoIncrement = false;
	private boolean autoDecrement = false;
	private boolean flingAutoIncrement = false;
	private float flingAutoIncrementVelocity = 0;

	private final int FLING_INCREMENT_THRESHOLD = 1;
	private final float FLING_SCALE_FACTOR = 50;
	private final float FLING_DECAY_FACTOR = 0.90f;
	
	private OnValueChangedListener valueChangedListener = null;

	/**
	 * This little guy handles the auto part of the auto incrementing feature.
	 * In doing so it instantiates itself. There has to be a pattern name for
	 * that...
	 * 
	 * @author Jeffrey F. Cole
	 * 
	 */
	Runnable repetetiveUpdater = new Runnable() {
		public void run() {
			if (autoIncrement) {
				increment();
				repeatUpdateHandler.postDelayed(this, REPEAT_DELAY);
			} else if (autoDecrement) {
				decrement();
				repeatUpdateHandler.postDelayed(this, REPEAT_DELAY);
			} else if (flingAutoIncrement) {
				float absVel = Math.abs(flingAutoIncrementVelocity);
				Log.d(TAG,
						"fling auto increment, flingAutoIncrementVelocity = "
								+ flingAutoIncrementVelocity);
				if (absVel > FLING_INCREMENT_THRESHOLD) {
					int by = (int) Math.floor(flingAutoIncrementVelocity
							/ FLING_SCALE_FACTOR);
					if (by == 0) {
						by = (flingAutoIncrementVelocity < 1) ? -1 : 1;
					}
					Log.d(TAG, "by = " + by);
					incrementBy(by);
					flingAutoIncrementVelocity *= FLING_DECAY_FACTOR;
					repeatUpdateHandler.postDelayed(this, REPEAT_DELAY);
				} else {
					flingAutoIncrement = false;
					Log.d(TAG, "deactivating fling auto increment");
				}
			}
		}
	};

	private final ViewConfiguration vc = ViewConfiguration.get(getContext());

	private final int SWIPE_MIN_DISTANCE = vc.getScaledTouchSlop();
	private final int SWIPE_MAX_OFF_PATH = vc.getScaledTouchSlop();
	private final int SWIPE_THRESHOLD_VELOCITY = vc
			.getScaledMinimumFlingVelocity();
	private GestureDetector gestureDetector;

	private View.OnLongClickListener longPressHandler = null;

	private OnTouchListener scrollFlingListener = new OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			Log.d(TAG, "scrollFlingListener got event");
			// if we are coasting from flinging and touch, stop coast.
			if (flingAutoIncrement) {
				flingAutoIncrement = false;
			}
			return gestureDetector.onTouchEvent(event);
		}
	};

	class MyGestureDetector extends SimpleOnGestureListener {

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			try {
				Log.d(TAG, "onFling");
				if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
					return false;
				// right to left swipe
				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					// Toast.makeText(SelectFilterActivity.this, "Left Swipe",
					// Toast.LENGTH_SHORT).show();
					Log.d(TAG, "Left Swipe, velocityx=" + velocityX);
					flingAutoIncrement = true;
					flingAutoIncrementVelocity = -1 * velocityX;
					repeatUpdateHandler.post(repetetiveUpdater);
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					// Toast.makeText(SelectFilterActivity.this, "Right Swipe",
					// Toast.LENGTH_SHORT).show();
					Log.d(TAG, "Right Swipe, velocityx=" + velocityX);
					flingAutoIncrement = true;
					flingAutoIncrementVelocity = -1 * velocityX;
					repeatUpdateHandler.post(repetetiveUpdater);
				}
			} catch (Exception e) {
				// nothing
			}
			return false;
		}

	}

	public NumberPicker(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
		//this.setLayoutParams(new LinearLayout.LayoutParams(
		//		LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		//this.setLayoutParams(new LinearLayout.LayoutParams(context, attributeSet));
		LayoutParams elementParams = new LinearLayout.LayoutParams(
				ELEMENT_HEIGHT, ELEMENT_WIDTH);

		// Gesture detection
		MyGestureDetector mgd = new MyGestureDetector();
		gestureDetector = new GestureDetector(mgd);
		this.setOnTouchListener(scrollFlingListener);
		// init the individual elements
		initDecrementButton(context);
		initValueText(context);
		initIncrementButton(context);
		parseAttrs(attributeSet);

		// Can be configured to be vertical or horizontal
		// Thanks for the help, LinearLayout!
		if (getOrientation() == VERTICAL) {
			addView(increment, elementParams);
			addView(valueText, elementParams);
			addView(decrement, elementParams);
		} else {
			addView(decrement, elementParams);
			addView(valueText, elementParams);
			addView(increment, elementParams);
		}
	}

	private void parseAttrs(AttributeSet attrs) {
		int attrCount = attrs.getAttributeCount();
		for (int i = 0; i < attrCount; i++) {
			String attrName = attrs.getAttributeName(i);
			if ("defaultValue".equals(attrName)) {
				this.setValue(attrs.getAttributeIntValue(i, 0));
			} else if ("decrementLabel".equals(attrName)) {
				this.setDecrementLabel(attrs.getAttributeValue(i));
			} else if ("incrementLabel".equals(attrName)) {
				this.setIncrementLabel(attrs.getAttributeValue(i));
			} else if ("minimumValue".equals(attrName)) {
				this.setMinimumValue(attrs.getAttributeIntValue(i, 0));
			} else if ("maximumValue".equals(attrName)) {
				this.setMaximumValue(attrs.getAttributeIntValue(i, 0));
			}

		}
	}

	private void setMaximumValue(int attributeIntValue) {
		this.maximum = attributeIntValue;
		if (this.getValue() > maximum) {
			this.setValue(maximum);
		}
	}

	public void setMinimumValue(int attributeIntValue) {
		this.minimum = attributeIntValue;
		if (this.getValue() < minimum) {
			this.setValue(minimum);
		}
	}

	private void initIncrementButton(Context context) {
		increment = new Button(context);
		increment.setTextSize(25);
		increment.setText("+");

		// Increment once for a click
		increment.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				increment();
			}
		});

		// Auto increment for a long click
		increment.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View arg0) {
				autoIncrement = true;
				repeatUpdateHandler.post(repetetiveUpdater);
				return false;
			}
		});

		// When the button is released, if we're auto incrementing, stop
		increment.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP && autoIncrement) {
					autoIncrement = false;
				} else {
					return scrollFlingListener.onTouch(v, event);
				}
				return false;
			}
		});
	}

	private void initValueText(Context context) {


		valueText = new TextView(context);
		valueText.setTextSize(25);

		valueText.setGravity(Gravity.CENTER_VERTICAL
				| Gravity.CENTER_HORIZONTAL);
		setValue(0);
		valueText.setInputType(InputType.TYPE_CLASS_NUMBER);
		valueText.setOnTouchListener(scrollFlingListener);
		valueText.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Log.d(TAG, "long press on value");
				if (longPressHandler != null) {
					return longPressHandler.onLongClick(v);
				}
				return false;
			}
		});
	}

	private void initDecrementButton(Context context) {
		decrement = new Button(context);
		decrement.setTextSize(25);
		decrement.setText("-");

		// Decrement once for a click
		decrement.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				decrement();
			}
		});

		// Auto Decrement for a long click
		decrement.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View arg0) {
				autoDecrement = true;
				repeatUpdateHandler.post(repetetiveUpdater);
				return true;
			}
		});

		// When the button is released, if we're auto decrementing, stop
		decrement.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP && autoDecrement) {
					autoDecrement = false;
				} else {
					return scrollFlingListener.onTouch(v, event);
				}
				return false;
			}
		});
	}

	public void increment() {
		if (value < maximum) {
			setValue(value+1);
		}
	}

	public void incrementBy(int by) {
		if (by > 0) {
			if (value + by < maximum) {
				setValue(value+by);
			} else {
				setValue(maximum);
			}
		} else {
			if (value + by > minimum) {
				setValue(value+by);
			} else {
				setValue(minimum);
			}
		}
	}

	public void decrement() {
		if (value > minimum) {
			setValue(value-1);
		}
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		if (value > maximum){
			value = maximum;
		}
		if (value < minimum){
			value = minimum;
		}
		this.value = value;
		valueText.setText(this.value.toString());
		if (this.valueChangedListener != null){
			this.valueChangedListener.onValueChanged(value);
		}
	}
	
	public void setOnValueChangedListener(OnValueChangedListener vcl){
		this.valueChangedListener = vcl;
	}

	public void setIncrementLabel(String label) {
		increment.setText(label);
	}

	public void setDecrementLabel(String label) {
		decrement.setText(label);
	}

	public void setLongClickHandler(View.OnLongClickListener listener) {
		this.longPressHandler = listener;
	}

}
