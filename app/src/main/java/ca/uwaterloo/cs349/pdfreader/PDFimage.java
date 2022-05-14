package ca.uwaterloo.cs349.pdfreader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.graphics.Canvas;
import java.util.ArrayList;
import java.util.Iterator;

@SuppressLint("AppCompatCustomView")
public class PDFimage extends ImageView {

    final String LOGNAME = "pdf_image";

    // drawing path
    Path path = null;
    ArrayList<Path> drawing_paths_1 = new ArrayList();
    ArrayList<Path> drawing_paths_2 = new ArrayList();
    ArrayList<Path> drawing_paths_3 = new ArrayList();
    ArrayList<Path> highlight_paths_1 = new ArrayList();
    ArrayList<Path> highlight_paths_2 = new ArrayList();
    ArrayList<Path> highlight_paths_3 = new ArrayList();
    ArrayList<String> undo_stack_1_reverse = new ArrayList();
    ArrayList<Path> undo_stack_1_reverse_path = new ArrayList();
    ArrayList<String> redo_stack_1 = new ArrayList();
    ArrayList<Path> redo_stack_1_path = new ArrayList();
    ArrayList<String> undo_stack_2_reverse = new ArrayList();
    ArrayList<Path> undo_stack_2_reverse_path = new ArrayList();
    ArrayList<String> redo_stack_2 = new ArrayList();
    ArrayList<Path> redo_stack_2_path = new ArrayList();
    ArrayList<String> undo_stack_3_reverse = new ArrayList();
    ArrayList<Path> undo_stack_3_reverse_path = new ArrayList();
    ArrayList<String> redo_stack_3 = new ArrayList();
    ArrayList<Path> redo_stack_3_path = new ArrayList();

    ScaleGestureDetector scale_detector;
    float scale_factor = 1.f;//orginally set the size of the scale size to be one

    // image to display
    Bitmap bitmap;
    Paint paint = new Paint();
    Path erase_path = null;
    Paint highlight_paint = new Paint();
    int current_page = 0;
    boolean draw = false;
    boolean erase = false;
    boolean highlight = false;
    boolean zoom_pan = false;
    float pan_start_x=0;
    float pan_start_y=0;
    float x_off_set=0;
    float y_off_set=0;
    float canvas_width=0;
    float canvas_height=0;
    Rect clipBounds_canvas;

    float pivot_x=0;
    float pivot_y=0;

    boolean two_finger = false;
    int mActivePointerId;


    // constructor
    public PDFimage(Context context) {
        super(context);
        //construct the scale detector for the futrue use
        scale_detector = new ScaleGestureDetector(context, new listener_scale());
    }

    // capture touch events (down/move/up) to create a path
    // and use that to create a stroke that we can draw
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1) {
            Log.d(LOGNAME, "Multitouch event");
        }
        Log.d(LOGNAME, "right: " + clipBounds_canvas.right + " left: " + clipBounds_canvas.left);
        Log.d(LOGNAME, "top: " + clipBounds_canvas.top + " bottom: " + clipBounds_canvas.bottom);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            //we need to consider the translation and scale of the canvas, so apply the formula
            //(event.getX())/scale_factor+ canvas_width, (event.getY())/scale_factor + canvas_height)
            case MotionEvent.ACTION_DOWN:
                Log.d(LOGNAME, "Action down");
                path = new Path();
                path.moveTo((event.getX())/scale_factor + canvas_width, (event.getY())/scale_factor + canvas_height);
                if (draw) {
                    if (current_page == 0) {
                        drawing_paths_1.add(path);
                        undo_stack_1_reverse.add("erase_last_draw");
                        undo_stack_1_reverse_path.add(null);
                    } else if (current_page == 1) {
                        drawing_paths_2.add(path);
                        undo_stack_2_reverse.add("erase_last_draw");
                        undo_stack_2_reverse_path.add(null);
                    } else if (current_page == 2) {
                        drawing_paths_3.add(path);
                        undo_stack_3_reverse.add("erase_last_draw");
                        undo_stack_3_reverse_path.add(null);
                    }
                } else if (highlight) {
                    if (current_page == 0) {
                        highlight_paths_1.add(path);
                        undo_stack_1_reverse.add("erase_last_highlight");
                        undo_stack_1_reverse_path.add(null);
                    } else if (current_page == 1) {
                        highlight_paths_2.add(path);
                        undo_stack_2_reverse.add("erase_last_highlight");
                        undo_stack_2_reverse_path.add(null);
                    } else if (current_page == 2) {
                        highlight_paths_3.add(path);
                        undo_stack_3_reverse.add("erase_last_highlight");
                        undo_stack_3_reverse_path.add(null);
                    }
                } else if (erase) {
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (!two_finger) {
                    Log.d(LOGNAME, "Action move");
                    path.lineTo((event.getX())/scale_factor+ canvas_width, (event.getY())/scale_factor + canvas_height);
                    if (erase) {
                        if (current_page == 0) {

                            Iterator<Path> i = drawing_paths_1.iterator();
                            while(i.hasNext()) {
                                //get the  path
                                Path path_temp = i.next();
                                //initialize a rectangle
                                RectF path_rect = new RectF();
                                //set the path bound as the rect
                                path_temp.computeBounds(path_rect, true);
                                Region path_region = new Region();
                                path_region.setPath(path_temp, new Region((int) path_rect.left, (int) path_rect.top, (int) path_rect.right, (int) path_rect.bottom));
                                Point point = new Point();
                                //apply the formula of the transition and scale
                                point.x = (int) ((event.getX()+ 0)/scale_factor+ canvas_width);
                                point.y = (int) ((event.getY()+ 0)/scale_factor + canvas_height);
                                invalidate();
                                Log.d(LOGNAME, "point: " + point);

                                //validate whether the point in the region formed by the path
                                if (path_region.contains((int) point.x, (int) point.y)) {
                                    Log.d(LOGNAME, "Touch IN");
                                    undo_stack_1_reverse.add("draw_path");
                                    undo_stack_1_reverse_path.add(path_temp);
                                    i.remove();
                                }
                                else {
                                    Log.d(LOGNAME, "Touch OUT");
                                }
                            }

                            i = highlight_paths_1.iterator();
                            while(i.hasNext()) {
                                Path path_temp = i.next();
                                RectF rectF = new RectF();
                                path_temp.computeBounds(rectF, true);
                                Region r = new Region();
                                r.setPath(path_temp, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));

                                Point point = new Point();
                                point.x = (int) event.getX();
                                point.y = (int) event.getY();
                                invalidate();
                                Log.d(LOGNAME, "point: " + point);

                                if (r.contains((int) point.x, (int) point.y)) {
                                    undo_stack_1_reverse.add("highlight_path");
                                    undo_stack_1_reverse_path.add(path_temp);
                                    Log.d(LOGNAME, "Touch IN");
                                    i.remove();
                                }
                                else {
                                    Log.d(LOGNAME, "Touch OUT");
                                }
                            }

                        } else if (current_page == 1) {

                            Iterator<Path> i = drawing_paths_2.iterator();
                            while(i.hasNext()) {
                                Path path = i.next();
                                RectF rectF = new RectF();
                                path.computeBounds(rectF, true);
                                Region r = new Region();
                                r.setPath(path, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));

                                Point point = new Point();
                                point.x = (int) event.getX();
                                point.y = (int) event.getY();
                                invalidate();
                                Log.d(LOGNAME, "point: " + point);

                                if (r.contains((int) point.x, (int) point.y)) {
                                    Log.d(LOGNAME, "Touch IN");
                                    undo_stack_2_reverse.add("draw_path");
                                    undo_stack_2_reverse_path.add(path);
                                    i.remove();
                                }
                                else {
                                    Log.d(LOGNAME, "Touch OUT");
                                }
                            }

                            i = highlight_paths_2.iterator();
                            while(i.hasNext()) {
                                Path path = i.next();
                                RectF rectF = new RectF();
                                path.computeBounds(rectF, true);
                                Region r = new Region();
                                r.setPath(path, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));

                                Point point = new Point();
                                point.x = (int) event.getX();
                                point.y = (int) event.getY();
                                invalidate();
                                Log.d(LOGNAME, "point: " + point);

                                if (r.contains((int) point.x, (int) point.y)) {
                                    Log.d(LOGNAME, "Touch IN");
                                    undo_stack_2_reverse.add("highlight_path");
                                    undo_stack_2_reverse_path.add(path);
                                    i.remove();
                                }
                                else {
                                    Log.d(LOGNAME, "Touch OUT");
                                }
                            }


                        } else if (current_page == 2) {

                            Iterator<Path> i = drawing_paths_3.iterator();
                            while(i.hasNext()) {
                                Path path = i.next();
                                RectF rectF = new RectF();
                                path.computeBounds(rectF, true);
                                Region r = new Region();
                                r.setPath(path, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));

                                Point point = new Point();
                                point.x = (int) event.getX();
                                point.y = (int) event.getY();
                                invalidate();
                                Log.d(LOGNAME, "point: " + point);

                                if (r.contains((int) point.x, (int) point.y)) {
                                    Log.d(LOGNAME, "Touch IN");
                                    undo_stack_3_reverse.add("draw_path");
                                    undo_stack_3_reverse_path.add(path);
                                    i.remove();
                                }
                                else {
                                    Log.d(LOGNAME, "Touch OUT");
                                }
                            }

                            i = highlight_paths_3.iterator();
                            while(i.hasNext()) {
                                Path path = i.next();
                                RectF rectF = new RectF();
                                path.computeBounds(rectF, true);
                                Region r = new Region();
                                r.setPath(path, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));

                                Point point = new Point();
                                point.x = (int) event.getX();
                                point.y = (int) event.getY();
                                invalidate();
                                Log.d(LOGNAME, "point: " + point);

                                if (r.contains((int) point.x, (int) point.y)) {
                                    Log.d(LOGNAME, "Touch IN");
                                    undo_stack_3_reverse.add("highlight_path");
                                    undo_stack_3_reverse_path.add(path);
                                    i.remove();
                                }
                                else {
                                    Log.d(LOGNAME, "Touch OUT");
                                }
                            }


                        }
                    }
                    break;
                } else {
                    if (zoom_pan) {
                        float x_1 = event.getX(0);
                        float y_1 = event.getY(0);
                        Log.d(LOGNAME, "First finger x_aix: " + x_1 + " y_axis: " + y_1);

                        float x_2 = event.getX(1);
                        float y_2 = event.getY(1);
                        Log.d(LOGNAME, "Second finger x_aix: " + x_2 + " y_axis: " + y_2);

                        //movement of the mid point
                        x_off_set = (x_1 + x_2) / 2 - pan_start_x;
                        y_off_set = (y_1 + y_2) / 2 - pan_start_y;
                        Log.d(LOGNAME, "Offset x_aix: " + x_off_set + " y_axis: "
                                + y_off_set);


                    }
                }

            case MotionEvent.ACTION_UP:
                Log.d(LOGNAME, "Action up");
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.d(LOGNAME, "another finger down");

                float x_1 = event.getX(0);
                float y_1 = event.getY(0);
                Log.d(LOGNAME, "First finger x_aix: " + x_1 + " y_axis: " + y_1);

                // Get the pointer's current position
                float x_2 = event.getX(1);
                float y_2 = event.getY(1);
                Log.d(LOGNAME, "Second finger x_aix: " + x_2 + " y_axis: " + y_2);

                pan_start_x = -x_off_set + (x_1 + x_2) / 2;
                pan_start_y = -y_off_set + (y_1 + y_2) / 2;
                pivot_x = (x_1+x_2)/2;
                pivot_y = (y_1+y_2)/2;
                Log.d(LOGNAME, "Current mid point x_aix: " + (x_1+x_2)/2 + " y_axis: "
                        + (y_1+y_2)/2);

                two_finger = true;
                break;

            case MotionEvent.ACTION_POINTER_UP:
                two_finger = false;
        }

        if (zoom_pan == true) {
            //if we are in the zoom mode,we will ask the build in class to handle that zoom event
            scale_detector.onTouchEvent(event);
        }




        return true;
    }

    // set image as background
    public void setImage(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    // set brush characteristics
    // e.g. color, thickness, alpha
    public void setBrush(Paint paint) {
        this.paint = paint;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //Log.d(LOGNAME, "Canvas height: " + canvas.getHeight() + " width: " + canvas.getWidth());
        //Log.d(LOGNAME, "Scale factor: " + scale_factor );
        clipBounds_canvas = canvas.getClipBounds();

        //order matters
        //first wee need to translate
        canvas.translate(x_off_set,y_off_set);
        //then we need to scaled
        //need to dividing by 2 to get the center of the current view
        canvas.scale(scale_factor, scale_factor,(canvas.getClipBounds().width()) / 2,(canvas.getClipBounds().height()) / 2);
        canvas_width = canvas.getClipBounds().left;
        canvas_height = canvas.getClipBounds().top;
        //Log.d(LOGNAME, "right: " + canvas.getClipBounds().right + " left: " + canvas.getClipBounds().left);
        //Log.d(LOGNAME, "top: " + canvas.getClipBounds().top + " bottom: " + canvas.getClipBounds().bottom);

        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(5);
        this.paint.setColor(Color.BLUE);

        this.highlight_paint.setStyle(Paint.Style.STROKE);
        this.highlight_paint.setStrokeWidth(30);
        this.highlight_paint.setColor(Color.YELLOW);
        this.highlight_paint.setAlpha(50);

        // draw background
        if (bitmap != null) {
            this.setImageBitmap(bitmap);
        }
        // draw lines over it
        if (current_page == 0) {
            for (Path path : drawing_paths_1) {
                canvas.drawPath(path, paint);
            }
            for (Path path : highlight_paths_1) {
                canvas.drawPath(path, highlight_paint);
            }
        } else if (current_page == 1) {
            for (Path path : drawing_paths_2) {
                canvas.drawPath(path, paint);
            }
            for (Path path : highlight_paths_2) {
                canvas.drawPath(path, highlight_paint);
            }
        } else if (current_page == 2) {
            for (Path path : drawing_paths_3) {
                canvas.drawPath(path, paint);
            }
            for (Path path : highlight_paths_3) {
                canvas.drawPath(path, highlight_paint);
            }
        }

        super.onDraw(canvas);
    }

    //read the ScaleGestureDetector from https://developer.android.com/training/gestures/scale
    private class listener_scale
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            //update the scale size
            scale_factor *= detector.getScaleFactor();

            //set a limitation to the zoom size we can get
            //should be smaller than the original size
            scale_factor = Math.max(1.0f, Math.min(scale_factor, 6.0f));
            Log.d(LOGNAME, "scale_factor:" + scale_factor);
            invalidate();
            return true;
        }
    }
}
