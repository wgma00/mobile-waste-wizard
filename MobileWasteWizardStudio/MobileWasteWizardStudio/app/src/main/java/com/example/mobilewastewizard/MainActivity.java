package com.example.mobilewastewizard;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * Main activity class that will delegate tasks  given from the user to their
 * proper modules.
 *  
 * @author William Granados 
 * @author Justin Li
 * @author Burhan Quadri
 *
 */
public class MainActivity extends ActionBarActivity {
  
  /** absolute path to image taken by user.*/
  private String imagePath = "";
  /** Database instance which handles user queries.*/
  private Database database;
  /** Window pop-up displayed to users.*/
  private PopupWindow popupWindow;

  /**
   * Initializes the main activity, fills auto-complete suggestions with database items.
   */
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    this.database = new Database(this);
    // fills the auto-complete selections
    List<String> autoCompleteList = database.getTotalList();
    ArrayAdapter<String> adapter =
        new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, autoCompleteList);

    AutoCompleteTextView actv = (AutoCompleteTextView) findViewById(R.id.search);
    actv.setAdapter(adapter);
    // receives query if coming from database activity
    Intent intent = getIntent();
    if (intent.getStringExtra(DatabaseActivity.EXTRA_ITEM) != "") {
      actv.setText(intent.getStringExtra(DatabaseActivity.EXTRA_ITEM));
    }
  }

  /**
   * Gets the query from the input.
   * 
   * @param view the view context where this is assigned to
   */
  public void getQuery(View view) {
    EditText searchQuery = (EditText) findViewById(R.id.search);
    CharSequence text = searchQuery.getText();
    // handles queries
    String query = text.toString();
    if (popupWindow != null) {
      popupWindow.dismiss();
    }
    // The user has entered something!
    // so now we work with their query
    if (!query.equals("")) {
      showResult(query);
    }
    hideSoftKeyBoard();
  }

  /**
   * Searches the database for query. If found, displays an image for the resulting bin. If not,
   * displays possible items from the database close to the initial query.
   * 
   * @param query Item to be searched for.
   */
  public void showResult(String query) {
    int duration = Toast.LENGTH_LONG;
    Context context = getApplicationContext();
    String bin = database.initialQuery(query);
    Toast toast = Toast.makeText(context, bin, duration);
    // The user query is found within the database
    if (bin != null) {
      toast.show();
      showImageResult(bin);
      MediaPlayer mp = MediaPlayer.create(context, R.raw.ding);
      mp.start();
    } else { // attempt to give relevant suggestions
      // finds the most relevant matches in the database
      List<String> suggestions = database.secondaryQuery(query);
      if (suggestions == null) {
        bin = "no bin";
        toast = Toast.makeText(context, bin, duration);
      } else {
        bin = "";
        for (int i = 0; i < suggestions.size(); i++) {
          bin += "Did you mean:" + suggestions.get(i) + "?\n";
        }
        toast = Toast.makeText(context, bin, duration);
      }
    }
    toast.show();
  }

  /**
   * Displays a pop-up window with an image for the corresponding result.
   * 
   * @param bin The resulting bin from the search results.
   */
  public void showImageResult(String bin) {
    // builds pop-up window
    LayoutInflater layoutInflater =
        (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
    View popupView = layoutInflater.inflate(R.layout.result_popup, null);
    popupWindow = new PopupWindow(popupView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    // displays result bin
    TextView resultText = (TextView) popupView.findViewById(R.id.resultText);
    resultText.setText(bin);
    bin = bin.replace(' ', '_');
    // gets image from assets folder
    InputStream is = null;
    try {
      is = getAssets().open("images/img_" + bin + ".png");
    } catch (IOException ioException) {
      System.out.println("images/img_" + bin + ".png");
    }
    ImageView result = (ImageView) popupView.findViewById(R.id.resultImg);
    result.setImageBitmap(BitmapFactory.decodeStream(is));
    // initializes dismiss button for pop-up
    Button btnDismiss = (Button) popupView.findViewById(R.id.dismiss);
    btnDismiss.setOnClickListener(new Button.OnClickListener() {
      public void onClick(View view) {
        popupWindow.dismiss();
      }
    });
    popupWindow.setBackgroundDrawable(null);
    AutoCompleteTextView actv = (AutoCompleteTextView) findViewById(R.id.search);
    // displays the pop-up
    popupWindow.showAsDropDown(actv, 0, 0);
  }

  /**
   * Receives a query from the built in speech recognition engine.
   * 
   * @param view the view context where this is assigned to
   */
  public void voice(View view) {
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
    try {
      startActivityForResult(intent, Constants.REQ_VOICE);
    } catch (Exception exception) {
      Toast.makeText(this, "Error initializing speech to text engine.", Toast.LENGTH_LONG).show();
    }
  }

  /**
   * Passes the recognized speech into the query search.
   */
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    // handles voice results
    if (requestCode == Constants.REQ_VOICE && resultCode == RESULT_OK) {
      ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
      String query = result.get(0);
      AutoCompleteTextView actv = (AutoCompleteTextView) findViewById(R.id.search);
      actv.setText(query);
    }

    //TODO implement image recognition
    // if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

    // }
  }

  /**
   * Switches to the database activity for manual search.
   * 
   * @param view the view context where this is assigned to
   */
  public void toDatabase(View view) {
    Intent intent = new Intent(this, DatabaseActivity.class);
    startActivity(intent);
  }

  private void hideSoftKeyBoard() {
    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    // verify if the soft keyboard is open
    if (imm.isAcceptingText()) {
      imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }
  }
}