package ca.uwaterloo.cs349.pdfreader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

// PDF sample code from
// https://medium.com/@chahat.jain0/rendering-a-pdf-document-in-android-activity-fragment-using-pdfrenderer-442462cb8f9a
// Issues about cache etc. are not at all obvious from documentation, so read this carefully.

public class MainActivity extends AppCompatActivity {

    final String LOGNAME = "pdf_viewer";
    final String FILENAME = "sample.pdf";
    final int FILERESID = R.raw.sample;
    int current_page = 0;


    // manage the pages of the PDF, see below
    PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfRenderer.Page currentPage;

    // custom ImageView class that captures strokes and draws them over the image
    PDFimage pageImage;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout layout = findViewById(R.id.pdfLayout);
        pageImage = new PDFimage(this);
        layout.addView(pageImage);
        layout.setEnabled(true);
        pageImage.setMinimumWidth(1000);
        pageImage.setMinimumHeight(2000);

        // open page 0 of the PDF
        // it will be displayed as an image in the pageImage (above)
        try {
            openRenderer(this);
            showPage(current_page);
        } catch (IOException exception) {
            Log.d(LOGNAME, "Error opening PDF");
        }

        Log.d(LOGNAME, "Width: " + pageImage.getWidth());
        Log.d(LOGNAME, "Height: :" + pageImage.getHeight());

        ImageButton next = (ImageButton) findViewById(R.id.next_page);
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (current_page + 1 > 2) {
                    return;
                } else {
                    TextView label = findViewById(R.id.textView4);
                    ++current_page;
                    int actual_page = current_page + 1;
                    String page_info = "Page "+ actual_page + "/3";
                    label.setText(page_info);
                    pageImage.current_page = current_page;
                    pageImage.pan_start_x=0;
                    pageImage.pan_start_y=0;
                    pageImage.x_off_set=0;
                    pageImage.y_off_set=0;
                    pageImage.canvas_width=0;
                    pageImage.canvas_height=0;
                    pageImage.scale_factor = 1.f;
                    showPage(current_page);
                }
            }
        });

        ImageButton up = (ImageButton) findViewById(R.id.previous_page);
        up.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (current_page - 1 < 0) {
                    return;
                } else {
                    TextView label = findViewById(R.id.textView4);
                    --current_page;
                    int actual_page = current_page + 1;
                    String page_info = "Page "+ actual_page + "/3";
                    label.setText(page_info);
                    pageImage.current_page = current_page;
                    pageImage.pan_start_x=0;
                    pageImage.pan_start_y=0;
                    pageImage.x_off_set=0;
                    pageImage.y_off_set=0;
                    pageImage.canvas_width=0;
                    pageImage.canvas_height=0;
                    pageImage.scale_factor = 1.f;
                    showPage(current_page);
                }
            }
        });

        ImageButton undo = (ImageButton) findViewById(R.id.undo);
        undo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int last_index = pageImage.undo_stack_1_reverse.size() - 1;
                int last_index_2 = pageImage.undo_stack_2_reverse.size() - 1;
                int last_index_3 = pageImage.undo_stack_3_reverse.size() - 1;
                if (current_page == 0 && last_index >= 0) {

                    if (pageImage.undo_stack_1_reverse.get(last_index).equals("erase_last_draw")) {
                        pageImage.redo_stack_1.add("re_draw_path");
                        pageImage.redo_stack_1_path.add(pageImage.drawing_paths_1.get((pageImage.drawing_paths_1.size()-1)));
                        pageImage.drawing_paths_1.remove(pageImage.drawing_paths_1.size()-1);//remove last path
                    } else if (pageImage.undo_stack_1_reverse.get(last_index).equals("erase_last_highlight")){
                        pageImage.redo_stack_1.add("re_highlight_path");
                        pageImage.redo_stack_1_path.add(pageImage.highlight_paths_1.get((pageImage.highlight_paths_1.size()-1)));
                        pageImage.highlight_paths_1.remove(pageImage.highlight_paths_1.size()-1);//remove last path
                    } else if (pageImage.undo_stack_1_reverse.get(last_index).equals("draw_path")) {
                        pageImage.drawing_paths_1.add(pageImage.undo_stack_1_reverse_path.get(last_index));
                        pageImage.redo_stack_1.add("erase_last_draw");
                        pageImage.redo_stack_1_path.add(null);
                    } else if (pageImage.undo_stack_1_reverse.get(last_index).equals("highlight_path")) {
                        pageImage.highlight_paths_1.add(pageImage.undo_stack_1_reverse_path.get(last_index));
                        pageImage.redo_stack_1.add("erase_last_highlight");
                        pageImage.redo_stack_1_path.add(null);
                    }
                    pageImage.undo_stack_1_reverse.remove(last_index);
                    pageImage.undo_stack_1_reverse_path.remove(last_index);

                } else if (current_page == 1 && last_index_2 >= 0) {

                    if (pageImage.undo_stack_2_reverse.get(last_index_2).equals("erase_last_draw")) {
                        pageImage.redo_stack_2.add("re_draw_path");
                        pageImage.redo_stack_2_path.add(pageImage.drawing_paths_2.get((pageImage.drawing_paths_2.size()-1)));
                        pageImage.drawing_paths_2.remove(pageImage.drawing_paths_2.size()-1);//remove last path
                    } else if (pageImage.undo_stack_2_reverse.get(last_index_2).equals("erase_last_highlight")){
                        pageImage.redo_stack_2.add("re_highlight_path");
                        pageImage.redo_stack_2_path.add(pageImage.highlight_paths_2.get((pageImage.highlight_paths_2.size()-1)));
                        pageImage.highlight_paths_2.remove(pageImage.highlight_paths_2.size()-1);//remove last path
                    } else if (pageImage.undo_stack_2_reverse.get(last_index_2).equals("draw_path")) {
                        pageImage.drawing_paths_2.add(pageImage.undo_stack_2_reverse_path.get(last_index_2));
                        pageImage.redo_stack_2.add("erase_last_draw");
                        pageImage.redo_stack_2_path.add(null);
                    } else if (pageImage.undo_stack_2_reverse.get(last_index_2).equals("highlight_path")) {
                        pageImage.highlight_paths_2.add(pageImage.undo_stack_2_reverse_path.get(last_index_2));
                        pageImage.redo_stack_2.add("erase_last_highlight");
                        pageImage.redo_stack_2_path.add(null);
                    }
                    pageImage.undo_stack_2_reverse.remove(last_index_2);
                    pageImage.undo_stack_2_reverse_path.remove(last_index_2);

                } else if (current_page == 2 && last_index_3 >= 0) {

                    if (pageImage.undo_stack_3_reverse.get(last_index_3).equals("erase_last_draw")) {
                        pageImage.redo_stack_3.add("re_draw_path");
                        pageImage.redo_stack_3_path.add(pageImage.drawing_paths_3.get((pageImage.drawing_paths_3.size()-1)));
                        pageImage.drawing_paths_3.remove(pageImage.drawing_paths_3.size()-1);//remove last path
                    } else if (pageImage.undo_stack_3_reverse.get(last_index_3).equals("erase_last_highlight")){
                        pageImage.redo_stack_3.add("re_highlight_path");
                        pageImage.redo_stack_3_path.add(pageImage.highlight_paths_3.get((pageImage.highlight_paths_3.size()-1)));
                        pageImage.highlight_paths_3.remove(pageImage.highlight_paths_3.size()-1);//remove last path
                    } else if (pageImage.undo_stack_3_reverse.get(last_index_3).equals("draw_path")) {
                        pageImage.drawing_paths_3.add(pageImage.undo_stack_3_reverse_path.get(last_index_3));
                        pageImage.redo_stack_3.add("erase_last_draw");
                        pageImage.redo_stack_3_path.add(null);
                    } else if (pageImage.undo_stack_3_reverse.get(last_index_3).equals("highlight_path")) {
                        pageImage.highlight_paths_3.add(pageImage.undo_stack_3_reverse_path.get(last_index_3));
                        pageImage.redo_stack_3.add("erase_last_highlight");
                        pageImage.redo_stack_3_path.add(null);
                    }
                    pageImage.undo_stack_3_reverse.remove(last_index_3);
                    pageImage.undo_stack_3_reverse_path.remove(last_index_3);

                }

            }
        });

        ImageButton redo = (ImageButton) findViewById(R.id.redo);
        redo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int last_index = pageImage.redo_stack_1.size() - 1;
                int last_index_2 = pageImage.redo_stack_2.size() - 1;
                int last_index_3 = pageImage.redo_stack_3.size() - 1;

                if (current_page == 0 && last_index >= 0) {

                    if (pageImage.redo_stack_1.get(last_index).equals("re_draw_path")) {
                        pageImage.undo_stack_1_reverse.add("erase_last_draw");
                        pageImage.undo_stack_1_reverse_path.add(null);
                        pageImage.drawing_paths_1.add(pageImage.redo_stack_1_path.get(last_index));//remove last path
                    } else if (pageImage.redo_stack_1.get(last_index).equals("re_highlight_path")){
                        pageImage.undo_stack_1_reverse.add("erase_last_highlight");
                        pageImage.undo_stack_1_reverse_path.add(null);
                        pageImage.highlight_paths_1.add(pageImage.redo_stack_1_path.get(last_index));//remove last path
                    } else if (pageImage.redo_stack_1.get(last_index).equals("erase_last_draw")) {
                        pageImage.undo_stack_1_reverse.add("draw_path");
                        pageImage.undo_stack_1_reverse_path.add(pageImage.drawing_paths_1.get((pageImage.drawing_paths_1.size()-1)));
                        pageImage.drawing_paths_1.remove(pageImage.drawing_paths_1.size()-1);//remove last path
                    } else if (pageImage.redo_stack_1.get(last_index).equals("erase_last_highlight")) {
                        pageImage.undo_stack_1_reverse.add("highlight_path");
                        pageImage.undo_stack_1_reverse_path.add(pageImage.highlight_paths_1.get((pageImage.highlight_paths_1.size()-1)));
                        pageImage.highlight_paths_1.remove(pageImage.highlight_paths_1.size()-1);//remove last path
                    }
                    pageImage.redo_stack_1.remove(last_index);
                    pageImage.redo_stack_1_path.remove(last_index);

                } else if (current_page == 1 && last_index_2 >= 0) {

                    if (pageImage.redo_stack_2.get(last_index_2).equals("re_draw_path")) {
                        pageImage.undo_stack_2_reverse.add("erase_last_draw");
                        pageImage.undo_stack_2_reverse_path.add(null);
                        pageImage.drawing_paths_2.add(pageImage.redo_stack_2_path.get(last_index_2));//remove last path
                    } else if (pageImage.redo_stack_2.get(last_index_2).equals("re_highlight_path")){
                        pageImage.undo_stack_2_reverse.add("erase_last_highlight");
                        pageImage.undo_stack_2_reverse_path.add(null);
                        pageImage.highlight_paths_2.add(pageImage.redo_stack_2_path.get(last_index_2));//remove last path
                    } else if (pageImage.redo_stack_2.get(last_index_2).equals("erase_last_draw")) {
                        pageImage.undo_stack_2_reverse.add("draw_path");
                        pageImage.undo_stack_2_reverse_path.add(pageImage.drawing_paths_2.get((pageImage.drawing_paths_2.size()-1)));
                        pageImage.drawing_paths_2.remove(pageImage.drawing_paths_2.size()-1);//remove last path
                    } else if (pageImage.redo_stack_2.get(last_index_2).equals("erase_last_highlight")) {
                        pageImage.undo_stack_2_reverse.add("highlight_path");
                        pageImage.undo_stack_2_reverse_path.add(pageImage.highlight_paths_2.get((pageImage.highlight_paths_2.size()-1)));
                        pageImage.highlight_paths_2.remove(pageImage.highlight_paths_2.size()-1);//remove last path
                    }
                    pageImage.redo_stack_2.remove(last_index_2);
                    pageImage.redo_stack_2_path.remove(last_index_2);

                } else if (current_page == 2 && last_index_3 >= 0) {

                    if (pageImage.redo_stack_3.get(last_index_3).equals("re_draw_path")) {
                        pageImage.undo_stack_3_reverse.add("erase_last_draw");
                        pageImage.undo_stack_3_reverse_path.add(null);
                        pageImage.drawing_paths_3.add(pageImage.redo_stack_3_path.get(last_index_3));//remove last path
                    } else if (pageImage.redo_stack_3.get(last_index_3).equals("re_highlight_path")){
                        pageImage.undo_stack_3_reverse.add("erase_last_highlight");
                        pageImage.undo_stack_3_reverse_path.add(null);
                        pageImage.highlight_paths_3.add(pageImage.redo_stack_3_path.get(last_index_3));//remove last path
                    } else if (pageImage.redo_stack_3.get(last_index_3).equals("erase_last_draw")) {
                        pageImage.undo_stack_3_reverse.add("draw_path");
                        pageImage.undo_stack_3_reverse_path.add(pageImage.drawing_paths_3.get((pageImage.drawing_paths_3.size()-1)));
                        pageImage.drawing_paths_3.remove(pageImage.drawing_paths_3.size()-1);//remove last path
                    } else if (pageImage.redo_stack_3.get(last_index_3).equals("erase_last_highlight")) {
                        pageImage.undo_stack_3_reverse.add("highlight_path");
                        pageImage.undo_stack_3_reverse_path.add(pageImage.highlight_paths_3.get((pageImage.highlight_paths_3.size()-1)));
                        pageImage.highlight_paths_3.remove(pageImage.highlight_paths_3.size()-1);//remove last path
                    }
                    pageImage.redo_stack_3.remove(last_index_3);
                    pageImage.redo_stack_3_path.remove(last_index_3);

                }

            }
        });
    }



    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStop() {
        super.onStop();
        try {
            closeRenderer();
        } catch (IOException ex) {
            Log.d(LOGNAME, "Unable to close PDF renderer");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), FILENAME);
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            InputStream asset = this.getResources().openRawResource(FILERESID);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        }
    }

    // do this before you quit!
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void closeRenderer() throws IOException {
        if (null != currentPage) {
            currentPage.close();
        }
        pdfRenderer.close();
        parcelFileDescriptor.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showPage(int index) {
        if (pdfRenderer.getPageCount() <= index) {
            return;
        }
        // Close the current page before opening another one.
        if (null != currentPage) {
            currentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        currentPage = pdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);

        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        // Display the page
        pageImage.setImage(bitmap);
    }
    // For attendance
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.zoom_pan:
                if (checked) {
                    Log.d(LOGNAME, "zoom_pan on");
                    pageImage.draw = false;
                    pageImage.erase = false;
                    pageImage.highlight = false;
                    pageImage.zoom_pan = true;
                    break;
                }
            case R.id.draw:
                if (checked) {
                    Log.d(LOGNAME, "draw on");
                    pageImage.draw = true;
                    pageImage.erase = false;
                    pageImage.highlight = false;
                    pageImage.zoom_pan = false;
                    if (current_page == 0) {
                        pageImage.redo_stack_1_path.clear();;
                        pageImage.redo_stack_1.clear();
                    } else if (current_page == 1) {
                        pageImage.redo_stack_2_path.clear();;
                        pageImage.redo_stack_2.clear();
                    } else if (current_page == 2) {
                        pageImage.redo_stack_3_path.clear();;
                        pageImage.redo_stack_3.clear();
                    }
                    break;
                }
            case R.id.erase:
                if (checked) {
                    Log.d(LOGNAME, "erase on");
                    pageImage.draw = false;
                    pageImage.erase = true;
                    pageImage.highlight = false;
                    pageImage.zoom_pan = false;
                    if (current_page == 0) {
                        pageImage.redo_stack_1_path.clear();;
                        pageImage.redo_stack_1.clear();
                    } else if (current_page == 1) {
                        pageImage.redo_stack_2_path.clear();;
                        pageImage.redo_stack_2.clear();
                    } else if (current_page == 2) {
                        pageImage.redo_stack_3_path.clear();;
                        pageImage.redo_stack_3.clear();
                    }
                    break;
                }
            case R.id.highlight:
                if (checked) {
                    Log.d(LOGNAME, "highlight on");
                    pageImage.draw = false;
                    pageImage.erase = false;
                    pageImage.highlight = true;
                    pageImage.zoom_pan = false;
                    if (current_page == 0) {
                        pageImage.redo_stack_1_path.clear();;
                        pageImage.redo_stack_1.clear();
                    } else if (current_page == 1) {
                        pageImage.redo_stack_2_path.clear();;
                        pageImage.redo_stack_2.clear();
                    } else if (current_page == 2) {
                        pageImage.redo_stack_3_path.clear();;
                        pageImage.redo_stack_3.clear();
                    }
                    break;
                }
        }
    }
}
