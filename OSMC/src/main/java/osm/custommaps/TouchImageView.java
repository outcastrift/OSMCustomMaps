package osm.custommaps;

/**
 * Created by Sam on 30-Jul-15.
 */
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;

/**
 * Extends Android ImageView to include pinch zooming and panning.
 */
public class TouchImageView extends ImageView
{
    private Bitmap image;
    private int imageW;
    private int imageH;
    private float centerX;    // in rotated image coordinates
    private float centerY;    // in rotated image coordinates
    private float scale = 1.0f;
    private VelocityTracker velocityTracker = null;
    private int rotation = 0;
    private Matrix imageRotation = new Matrix();
    private Matrix drawMatrix = new Matrix();

    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    // Remember some things for zooming
    PointF last = new PointF();
    PointF start = new PointF();
    float minScale = 1f;
    float maxScale = 3f;
    float[] m;

    float redundantXSpace, redundantYSpace;

    float width, height;
    static final int CLICK = 3;
    float saveScale = 1f;
    float right, bottom, origWidth, origHeight, bmWidth, bmHeight;
    private AnnotationLayer annotations;

    ScaleGestureDetector mScaleDetector;

    Context context;

    public TouchImageView(Context context)
    {
        super(context);
        sharedConstructing(context);
    }

    public TouchImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        sharedConstructing(context);
    }
    public void setBitmap(Bitmap bitmap) {
        image = bitmap;
        if (rotation != 0) {
            setOrientation(rotation);
        } else if (image != null) {
            imageW = image.getWidth();
            imageH = image.getHeight();
        }
        scale = 1.0f;
        resetCenter();
        postInvalidate();
    }

    /**
     * Sets the necessary rotation needed to display the image right side up
     *
     * @param rotateDegrees
     */
    public void setOrientation(int rotateDegrees) {
        imageRotation.reset();
        imageRotation.postRotate(rotateDegrees);
        rotation = rotateDegrees;
        if (image == null) {
            return;
        }
        switch (rotateDegrees) {
            case 0:
                imageW = image.getWidth();
                imageH = image.getHeight();
                break;
            case 90:
                imageRotation.postTranslate(image.getHeight(), 0);
                imageW = image.getHeight();
                imageH = image.getWidth();
                break;
            case 180:
                imageRotation.postTranslate(image.getWidth(), image.getHeight());
                imageW = image.getWidth();
                imageH = image.getHeight();
                break;
            case 270:
                imageRotation.postTranslate(0, image.getWidth());
                imageW = image.getHeight();
                imageH = image.getWidth();
                break;
        }
        postInvalidate();
    }

    /**
     * Apply the given multiplier to current image scale.
     *
     * @param multiplier to zoom by (>1 zooms in, <1 zooms out)
     */
    public void zoomBy(float multiplier) {
        setScale(multiplier * getScale());
    }

    /**
     * Sets the absolute scale factor for the image.
     *
     * @param scale size multiplier (1 = no scaling)
     */
    public void setScale(float scale) {
        this.scale = scale;
        postInvalidate();
    }

    /**
     * @return the current image scaling factor
     */
    public float getScale() {
        return scale;
    }

    /**
     * @return image coordinates that are in the center of the display
     */
    public PointF getCenterPoint() {
        PointF center = new PointF(centerX, centerY);
        Matrix m = new Matrix();
        imageRotation.invert(m);
        mapPoint(m, center);
        return center;
    }

    /**
     * Set the image coordinates at display center point
     */
    public void setCenterPoint(PointF p) {
        mapPoint(imageRotation, p);
        centerX = p.x;
        centerY = p.y;
        postInvalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (image == null || image.isRecycled()) {
            return;
        }
        checkImageOutOfBounds();
        computeDrawMatrix();
        if (annotations != null) {
            annotations.setDrawMatrix(drawMatrix);
        }
        canvas.drawBitmap(image, drawMatrix, null);
    }

    /**
     * Map a point using a matrix storing the result in the original point
     *
     * @param m Matrix used for conversion
     * @param p Point to be mapped
     */
    private void mapPoint(Matrix m, PointF p) {
        float[] point = new float[] { p.x, p.y };
        m.mapPoints(point);
        p.x = point[0];
        p.y = point[1];
    }

    /**
     * Centers the bitmap in this widget
     */
    private void resetCenter() {
        if (image != null) {
            centerX = imageW / 2f;
            centerY = imageH / 2f;
        } else {
            centerX = 0;
            centerY = 0;
        }
    }

    /**
     * Recomputes the matrix used for drawing the image (updates scale and
     * translation)
     */
    private void computeDrawMatrix() {
        drawMatrix.set(imageRotation);
        drawMatrix.postScale(scale, scale);
        drawMatrix.postTranslate(getWidth() / 2f - scale * centerX, getHeight() / 2f - scale * centerY);
    }

    /**
     * Checks if the image has scrolled too far and autoscroll needs to be
     * stopped.
     */
    private void checkImageOutOfBounds() {
        if (image == null || image.isRecycled()) {
            return;
        }
        boolean stopScroll = false;
        if (centerX < 0) {
            centerX = 0;
            stopScroll = true;
        } else if (centerX > imageW) {
            centerX = imageW;
            stopScroll = true;
        }
        if (centerY < 0) {
            centerY = 0;
            stopScroll = true;
        } else if (centerY > imageH) {
            centerY = imageH;
            stopScroll = true;
        }

        if (stopScroll) {
            inertiaScroller.stop();
        }
    }

    // --------------------------------------------------------------------------
    // Inertia scrolling

    private InertiaScroller inertiaScroller = new InertiaScroller();

    private class InertiaScroller implements Runnable {
        private static final float friction = 5f;

        private float xv = 0f;
        private float yv = 0f;
        private float xFriction = 0f;
        private float yFriction = 0f;

        public void start(float xv, float yv) {
            float speed = FloatMath.sqrt(xv * xv + yv * yv);
            float percent = friction / speed;
            xFriction = percent * xv;
            yFriction = percent * yv;
            this.xv = xv;
            this.yv = yv;
            postDelayed(this, 50);
        }

        public void stop() {
            xv = yv = xFriction = yFriction = 0;
        }

        @Override
        public void run() {
            centerX += xv / scale;
            centerY += yv / scale;
            invalidate();

            if (xv == 0 || (xv < 0 && xv >= xFriction) || (xv > 0 && xv <= xFriction)) {
                xv = yv = 0;
            } else {
                xv -= xFriction;
                yv -= yFriction;
                postDelayed(this, 50);
            }
        }
    }

    public void setAnnotationLayer(AnnotationLayer annotations) {
        this.annotations = annotations;
    }

    private void sharedConstructing(Context context)
    {
        super.setClickable(true);
        this.context = context;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        drawMatrix.setTranslate(1f, 1f);
        m = new float[9];
        setImageMatrix(drawMatrix);
        setScaleType(ScaleType.MATRIX);

        setOnTouchListener(new OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                mScaleDetector.onTouchEvent(event);

                drawMatrix.getValues(m);
                float x = m[Matrix.MTRANS_X];
                float y = m[Matrix.MTRANS_Y];
                PointF curr = new PointF(event.getX(), event.getY());

                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        last.set(event.getX(), event.getY());
                        start.set(last);
                        mode = DRAG;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mode == DRAG)
                        {
                            float deltaX = curr.x - last.x;
                            float deltaY = curr.y - last.y;
                            float scaleWidth = Math.round(origWidth * saveScale);
                            float scaleHeight = Math.round(origHeight * saveScale);
                            if (scaleWidth < width)
                            {
                                deltaX = 0;
                                if (y + deltaY > 0)
                                    deltaY = -y;
                                else if (y + deltaY < -bottom)
                                    deltaY = -(y + bottom);
                            }
                            else if (scaleHeight < height)
                            {
                                deltaY = 0;
                                if (x + deltaX > 0)
                                    deltaX = -x;
                                else if (x + deltaX < -right)
                                    deltaX = -(x + right);
                            }
                            else
                            {
                                if (x + deltaX > 0)
                                    deltaX = -x;
                                else if (x + deltaX < -right)
                                    deltaX = -(x + right);

                                if (y + deltaY > 0)
                                    deltaY = -y;
                                else if (y + deltaY < -bottom)
                                    deltaY = -(y + bottom);
                            }
                            drawMatrix.postTranslate(deltaX, deltaY);
                            last.set(curr.x, curr.y);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        mode = NONE;
                        int xDiff = (int) Math.abs(curr.x - start.x);
                        int yDiff = (int) Math.abs(curr.y - start.y);
                        if (xDiff < CLICK && yDiff < CLICK)
                            performClick();
                        break;

                    case MotionEvent.ACTION_POINTER_UP:
                        mode = NONE;
                        break;
                }
                setImageMatrix(drawMatrix);
                invalidate();
                return true; // indicate event was handled
            }
        });
    }

    @Override
    public void setImageBitmap(Bitmap bm)
    {
        super.setImageBitmap(bm);
        if (bm != null)
        {
            bmWidth = bm.getWidth();
            bmHeight = bm.getHeight();
        }
    }

    public void setMaxZoom(float x)
    {
        maxScale = x;
    }

    private class ScaleListener extends
            ScaleGestureDetector.SimpleOnScaleGestureListener
    {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector)
        {
            mode = ZOOM;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector)
        {
            float mScaleFactor = (float) Math.min(
                    Math.max(.95f, detector.getScaleFactor()), 1.05);
            float origScale = saveScale;
            saveScale *= mScaleFactor;
            if (saveScale > maxScale)
            {
                saveScale = maxScale;
                mScaleFactor = maxScale / origScale;
            }
            else if (saveScale < minScale)
            {
                saveScale = minScale;
                mScaleFactor = minScale / origScale;
            }
            right = width * saveScale - width - (2 * redundantXSpace * saveScale);
            bottom = height * saveScale - height
                    - (2 * redundantYSpace * saveScale);
            if (origWidth * saveScale <= width || origHeight * saveScale <= height)
            {
                drawMatrix.postScale(mScaleFactor, mScaleFactor, width / 2, height / 2);
                if (mScaleFactor < 1)
                {
                    drawMatrix.getValues(m);
                    float x = m[Matrix.MTRANS_X];
                    float y = m[Matrix.MTRANS_Y];
                    if (mScaleFactor < 1)
                    {
                        if (Math.round(origWidth * saveScale) < width)
                        {
                            if (y < -bottom)
                                drawMatrix.postTranslate(0, -(y + bottom));
                            else if (y > 0)
                                drawMatrix.postTranslate(0, -y);
                        }
                        else
                        {
                            if (x < -right)
                                drawMatrix.postTranslate(-(x + right), 0);
                            else if (x > 0)
                                drawMatrix.postTranslate(-x, 0);
                        }
                    }
                }
            }
            else
            {
                drawMatrix.postScale(mScaleFactor, mScaleFactor, detector.getFocusX(),
                        detector.getFocusY());
                drawMatrix.getValues(m);
                float x = m[Matrix.MTRANS_X];
                float y = m[Matrix.MTRANS_Y];
                if (mScaleFactor < 1)
                {
                    if (x < -right)
                        drawMatrix.postTranslate(-(x + right), 0);
                    else if (x > 0)
                        drawMatrix.postTranslate(-x, 0);
                    if (y < -bottom)
                        drawMatrix.postTranslate(0, -(y + bottom));
                    else if (y > 0)
                        drawMatrix.postTranslate(0, -y);
                }
            }
            return true;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
        // Fit to screen.
        float scale;
        float scaleX = (float) width / (float) bmWidth;
        float scaleY = (float) height / (float) bmHeight;
        scale = Math.min(scaleX, scaleY);
        drawMatrix.setScale(scale, scale);
        setImageMatrix(drawMatrix);
        saveScale = 1f;

        // Center the image
        redundantYSpace = (float) height - (scale * (float) bmHeight);
        redundantXSpace = (float) width - (scale * (float) bmWidth);
        redundantYSpace /= (float) 2;
        redundantXSpace /= (float) 2;

        drawMatrix.postTranslate(redundantXSpace, redundantYSpace);

        origWidth = width - 2 * redundantXSpace;
        origHeight = height - 2 * redundantYSpace;
        right = width * saveScale - width - (2 * redundantXSpace * saveScale);
        bottom = height * saveScale - height - (2 * redundantYSpace * saveScale);
        setImageMatrix(drawMatrix);
    }
}
