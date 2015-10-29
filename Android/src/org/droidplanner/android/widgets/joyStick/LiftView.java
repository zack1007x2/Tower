package org.droidplanner.android.widgets.joyStick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class LiftView extends View implements Runnable {
	// Constants
	private final double RAD = 57.2957795;
	public final static long DEFAULT_LOOP_INTERVAL = 500; // 200 ms
	public final static int FRONT = 3;
	public final static int FRONT_RIGHT = 2;
	public final static int RIGHT = 1;
	public final static int RIGHT_BOTTOM = 8;
	public final static int BOTTOM = 7;
	public final static int BOTTOM_LEFT = 6;
	public final static int LEFT = 5;
	public final static int LEFT_FRONT = 4;
	// Variables
	private OnLiftPowerMoveListener onLiftPowerMoveListener; // Listener
	private Thread thread = new Thread(this);
	private long loopInterval = DEFAULT_LOOP_INTERVAL;
	private int xPosition = 0; // Touch x position
	private int yPosition = 0; // Touch y position
	private double centerX = 0; // Center view x position
	private double centerY = 0; // Center view y position
	private Paint mainRect;
//	private Paint secondaryCircle;
	private Paint button;
	private Paint horizontalLine;
	private Paint verticalLine;
	private int joystickRadius;
	private int buttonRadius;
	private int lastAngle = 0;
	private int lastPower = 0;

	private int viewHeight, viewWeight;

	public LiftView(Context context) {
		super(context);
	}

	public LiftView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initJoystickView();
	}

	public LiftView(Context context, AttributeSet attrs, int defaultStyle) {
		super(context, attrs, defaultStyle);
		initJoystickView();
	}

	protected void initJoystickView() {

		mainRect = new Paint(Paint.ANTI_ALIAS_FLAG);
		mainRect.setColor(Color.TRANSPARENT);
		mainRect.setStyle(Paint.Style.FILL_AND_STROKE);

		verticalLine = new Paint();
		verticalLine.setStrokeWidth(5);
		verticalLine.setColor(Color.RED);

		horizontalLine = new Paint();
		horizontalLine.setStrokeWidth(2);
		horizontalLine.setColor(Color.BLACK);

		button = new Paint(Paint.ANTI_ALIAS_FLAG);
		button.setColor(Color.RED);
		button.setStyle(Paint.Style.FILL);
	}

	@Override
	protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
		super.onSizeChanged(xNew, yNew, xOld, yOld);
		xPosition = (int) getWidth() / 2;
		buttonRadius = (int) (yNew / 2 * 0.20);
		joystickRadius = (int) (yNew / 2 * 0.80);
		yPosition = (int) getHeight() - buttonRadius;

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// super.onDraw(canvas);
		centerX = getWidth() / 2;
		centerY = getHeight() / 2;
		//		centerY=0;
		// painting the main rect
		Rect rect = new Rect((int)centerX-10,(int)(centerY+joystickRadius),(int)centerX+10,
				(int)(centerY-joystickRadius));
		canvas.drawRect(rect,mainRect);
		// painting the secondary circle
//		canvas.drawCircle((int) centerX, (int) centerY, joystickRadius / 2, secondaryCircle);
		// paint lines
		canvas.drawLine((float) centerX, (float)(centerY+joystickRadius), (float) centerX,(float)(centerY -joystickRadius), verticalLine);
		canvas.drawLine((float) (centerX - 10), (float) centerY, (float) (centerX +10), (float) centerY, horizontalLine);
//		canvas.drawLine((float) centerX, (float) (centerY + joystickRadius), (float) centerX, (float) centerY, horizontalLine);

		// painting the move button
		canvas.drawCircle(getWidth() / 2, yPosition, buttonRadius, button);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		this.getParent().requestDisallowInterceptTouchEvent(true);
		xPosition = (int) event.getX();
		yPosition = (int) event.getY();
		double abs = Math.sqrt((xPosition - centerX) * (xPosition - centerX) + (yPosition - centerY) * (yPosition - centerY));
		if (abs > joystickRadius) {
			xPosition = (int) ((xPosition - centerX) * joystickRadius / abs + centerX);
			yPosition = (int) ((yPosition - centerY) * joystickRadius / abs + centerY);
		}
		invalidate();
		if (event.getAction() == MotionEvent.ACTION_UP) {
			xPosition = (int) centerX;
			//			yPosition = (int) centerY;

			thread.interrupt();
			if (onLiftPowerMoveListener != null) {
				onLiftPowerMoveListener.onValueChanged(getAngle(), getPower(), getDirection());
				onLiftPowerMoveListener.onNotTouch();
			}

		}
		if (onLiftPowerMoveListener != null && event.getAction() == MotionEvent.ACTION_DOWN) {
			if (thread != null && thread.isAlive()) {
				thread.interrupt();
			}
			thread = new Thread(this);
			thread.start();
			if (onLiftPowerMoveListener != null) onLiftPowerMoveListener.onValueChanged(getAngle(), getPower(), getDirection());
		}
		return true;
	}

	private int getAngle() {
		if (xPosition > centerX) {
			if (yPosition < centerY) {
				return lastAngle = (int) (Math.atan((yPosition - centerY) / (xPosition - centerX)) * RAD + 90);
			}
			else if (yPosition > centerY) {
				return lastAngle = (int) (Math.atan((yPosition - centerY) / (xPosition - centerX)) * RAD) + 90;
			}
			else {
				return lastAngle = 90;
			}
		}
		else if (xPosition < centerX) {
			if (yPosition < centerY) {
				return lastAngle = (int) (Math.atan((yPosition - centerY) / (xPosition - centerX)) * RAD - 90);
			}
			else if (yPosition > centerY) {
				return lastAngle = (int) (Math.atan((yPosition - centerY) / (xPosition - centerX)) * RAD) - 90;
			}
			else {
				return lastAngle = -90;
			}
		}
		else {
			if (yPosition <= centerY) {
				return lastAngle = 0;
			}
			else {
				if (lastAngle < 0) {
					return lastAngle = -180;
				}
				else {
					return lastAngle = 180;
				}
			}
		}
	}

	public int getPower() {
		return (int) (100 * Math.sqrt((xPosition - centerX) * (xPosition - centerX) + (yPosition - centerY) * (yPosition - centerY)) / joystickRadius);
	}

	public int getDirection() {

		int direction = 0;

		double x_dir = centerX - xPosition;
		double y_dir = centerY - yPosition;

		if (x_dir > 0) {
			if (y_dir > 0) {
				//left top
				direction = LEFT_FRONT;
			}
			else if (y_dir < 0) {
				//left bottom
				direction = BOTTOM_LEFT;
			}
			else {
				//left
				direction = LEFT;
			}
		}
		else if (x_dir < 0) {
			if (y_dir > 0) {
				//right top
				direction = FRONT_RIGHT;
			}
			else if (y_dir < 0) {
				//right bottom
				direction = RIGHT_BOTTOM;
			}
			else {
				//right
				direction = RIGHT;
			}
		}
		else {
			if (y_dir > 0) {
				//top
				direction = FRONT;
			}
			else if (y_dir < 0) {
				//bottom
				direction = BOTTOM;
			}
		}

		return direction;
	}

	public void setOnLiftPowerMoveListener(OnLiftPowerMoveListener listener, long repeatInterval) {
		this.onLiftPowerMoveListener = listener;
		this.loopInterval = repeatInterval;
	}

	public interface OnLiftPowerMoveListener {
		void onValueChanged(int angle, int power, int direction);

		void onNotTouch();
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			post(new Runnable() {
				public void run() {
					if (onLiftPowerMoveListener != null) onLiftPowerMoveListener.onValueChanged(getAngle(), getPower(), getDirection());
				}
			});
			try {
				Thread.sleep(loopInterval);
			}
			catch (InterruptedException e) {
				break;
			}
		}
	}
}