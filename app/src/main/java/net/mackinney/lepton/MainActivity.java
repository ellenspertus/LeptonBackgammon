package net.mackinney.lepton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;  // https://stackoverflow.com/questions/31297246/activity-appcompatactivity-fragmentactivity-and-actionbaractivity-when-to-us
// import androidx.fragment.app.FragmentActivity; // AppCompatActivity extends FragmentActivity


import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The main activity for Lepton Backgammon, listener for GameHelper, manages all buttons.
 */
public class MainActivity extends AppCompatActivity implements GameHelperListener {

    public static final int CONSOLE_TEXTSIZE = 2048;

    private final String TAG = "MainActivity";
    // HINT save state info: https://stackoverflow.com/questions/151777/how-to-save-an-activity-state-using-save-instance-state
    // INITIALIZATION BELONGS IN METHOD (EXCEPTION CONTROL)
    GameHelper helper;
    private TextView consoleTextView;
    private EditText commandText;
    private BoardView boardView;
    private TextView oppScore;
    private TextView playerScore;
    private PreferencesManager preferences;
    private static final Board GONE = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        Log.i(TAG, "oncreate called.");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main); // must come BEFORE findViewById commands
//  WHY WAS THIS EVER HERE?
//        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            Log.i(TAG, "getResources");
//        }
        // Programmatically hide titlebar:
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // getSupportActionBar().hide();
        // But, I'm doing it by applying a theme named NoActionBar
        preferences = new PreferencesManager(this.getApplicationContext());
        commandText = findViewById(R.id.commandText);
        consoleTextView = findViewById(R.id.console);
        consoleTextView.setMovementMethod(new ScrollingMovementMethod());
        consoleTextView.setTextColor(Color.BLACK);
        boardView = findViewById(R.id.boardView);
        oppScore = findViewById(R.id.oppScore);
        playerScore = findViewById(R.id.playerScore);
        helper = new GameHelper(this, this.getApplicationContext());
        boardView.initialize(helper);
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder log=new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line);
            }

        } catch (IOException e) {
        }
    }

    private void logcatSaver() {

    }
    /**
     * Called when the user taps the Send button
     */
    public void sendCommand(View view) {
        String cmd = commandText.getText().toString();
        commandText.setText("");
        helper.addCommand(cmd);
    }

    /**
     * This button is always visible, its name depends on the Gammon Board's visibility
     *  - Its name is Console when the Board is visible
     *  - Its name is Board when the Board is hidden
     * When the Gammon Board is hidden, the command field and Send buttons become visible
     * @param view the Show/Hide button
     */
    public void showHideBoardView(View view) {
        //Button btn = findViewById(R.id.showHide);
        Button btn = (Button) view;
        if (boardView.getVisibility() == View.VISIBLE) {
            boardView.setVisibility(View.INVISIBLE);
            consoleTextView.setVisibility(View.VISIBLE);
            oppScore.setVisibility(View.INVISIBLE);
            playerScore.setVisibility(View.INVISIBLE);
            ((Button) findViewById(R.id.send)).setVisibility(View.VISIBLE);
            btn.setText(R.string.button_show);
        } else {
            updateGameBoard(null);
            boardView.setVisibility(View.VISIBLE);
            consoleTextView.setVisibility(View.INVISIBLE);
            oppScore.setVisibility(View.VISIBLE);
            playerScore.setVisibility(View.VISIBLE);
            ((Button) findViewById(R.id.send)).setVisibility(View.GONE);
            btn.setText(R.string.button_hide);
        }
//        updateScoreboard();
    }

    private void toggleVisibility(View view) {

    }

    public void join(View view) {
        helper.addCommand("join");
    }

    public void invite(View view) {
        // create an alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.invite_title);
        // set the custom layout and customize it
        final View inviteLayout = getLayoutInflater().inflate(R.layout.invite, null); // use of null is not recommended, but I don't know what else to put here.
        ((EditText) inviteLayout.findViewById(R.id.opponent)).setText(helper.getReady());
        final int invitationLength = preferences.getInvitationLength();
        ((EditText) inviteLayout.findViewById(R.id.game_length)).setText(Integer.toString(invitationLength)); // "" + int is an abstraction violation.
        builder.setView(inviteLayout);
        // add a button
        builder.setPositiveButton("Invite", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // send data from the AlertDialog to the Activity
                String opp = ((EditText) inviteLayout.findViewById(R.id.opponent)).getText().toString();
                String gl = ((EditText) inviteLayout.findViewById(R.id.game_length)).getText().toString();
                if (Integer.parseInt(gl) != invitationLength) {
                    preferences.setInvitationLength(Integer.parseInt(gl));
                    preferences.commit();
                }
                helper.addCommand("invite " + opp + " " + gl);
            }
        });
        builder.setNegativeButton("Cancel", null);
        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // https://medium.com/@suragch/adding-a-list-to-an-android-alertdialog-e13c1df6cf00
    // TODO hide this button when not playing
    public void resign(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Resign the game?");
        String[] choices = {"Normal", "Gammon", "Backgammon"};
        final int[] checkedItem = {0}; // Normal TODO: writeup why this had to be final, use of array
        builder.setSingleChoiceItems(choices, checkedItem[0], new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkedItem[0] = which;
            }
        });
        // add OK and Cancel buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handleResignation("Player", checkedItem[0]);
            }
        });
        builder.setNegativeButton("Cancel", null);
        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // https://stackoverflow.com/questions/6173400/how-to-hide-a-button-programmatically
    //    playButton = (Button) findViewById(R.id.play);
    //    playButton.setVisibility(View.VISIBLE);
    //    playButton.setOnClickListener(new OnClickListener() {
    //      @Override
    //      public void onClick(View v) {
    //          //when play is clicked show stop button and hide play button
    //          playButton.setVisibility(View.GONE);
    //          stopButton.setVisibility(View.VISIBLE);
    //      }
    //    });
    public void toggleAutoroll(View view) {
        helper.addCommand("toggle double");
        helper.roll();
    }

    // https://suragch.medium.com/creating-a-custom-alertdialog-bae919d2efa5

    /**
     * Displays a login dialog with stored user name and password.
     * On Connect, the displayed user name and login are saved, and a login to FIBS is attempted
     * with these credentials.
     *
     * The TelnetHandler will call back to update the button name after a successful login,
     * or whenever a logout is attempted.
     *
     * @param view
     */
    public void loginLogout(View view) {
        Button btn = (Button) view;
        if ("Login".equals(btn.getText())) {
            // create an alert builder
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Login Info");
            // Set the custom layout and customize it. Final because the procedure is to be called
            // later; the customlayout is external to the procedure and Java requires free variables
            // in lambdas to be constant.
            final View loginLayout = getLayoutInflater().inflate(R.layout.login, null);
            ((EditText) loginLayout.findViewById(R.id.userName)).setText(preferences.getUser());
            ((EditText) loginLayout.findViewById(R.id.password)).setText(preferences.getPassword());
            builder.setView(loginLayout);
            // add a button
            builder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // send data from the AlertDialog to the Activity
                    EditText userName = (EditText) loginLayout.findViewById(R.id.userName);
                    String name = userName.getText().toString();
                    EditText password = (EditText) loginLayout.findViewById(R.id.password);
                    String pw = password.getText().toString();
                    updateLoginData(userName.getText().toString(), password.getText().toString());
                    helper.addCommand("connect " + name + " " + pw);
                }
            });
            builder.setNegativeButton("Cancel", null);
            // create and show the alert dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        } else { // btn.getText() == Logout
            helper.addCommand("bye");
            setScoreBoardMessage(GONE);
            boardView.setBackgroundSplash();
        }
    }

    // Set the button name to "Logout" if logged in and vice versa TODO: Thesis writeup, why runOnUIThread for some, not login button
    @Override
    public void updateLoginButton(final boolean isLoggedIn) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int visibility;
                if (isLoggedIn) {
                    ((Button) findViewById(R.id.loginLogout)).setText(R.string.button_logout);
                    visibility = View.VISIBLE;
                } else {
                    ((Button) findViewById(R.id.loginLogout)).setText(R.string.button_login);
                    visibility = View.GONE;
                }
                findViewById(R.id.autoroll).setVisibility(visibility);
                findViewById(R.id.resign).setVisibility(visibility);
                findViewById(R.id.invite).setVisibility(visibility);
                findViewById(R.id.join).setVisibility(visibility);
            }
        });
    }

    // do something with the data coming from the AlertDialog
    private void updateLoginData(String username, String password) {
        preferences.setUser(username);
        preferences.setPassword(password);
        preferences.commit();
    }

    @Override
    public void updateGameBoard(final Board board) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boardView.updateGameBoard(board);
            }
        });
    }

    @Override
    public void setScoreBoardMessage(final Board board) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (board != null) {
                    oppScore.setText(board.getOppName() + "\n" + board.getState(board.SCORE_OPPONENT) + "/" + board.getState(board.MATCH_LENGTH) + (board.isGameOver() ? " Final" : ""));
                    playerScore.setText(helper.getPlayerName() + "\n" + board.getState(board.SCORE_PLAYER) + "/" + board.getState(board.MATCH_LENGTH));
                    oppScore.setVisibility(View.VISIBLE);
                    oppScore.invalidate();
                    playerScore.setVisibility(View.VISIBLE);
                    playerScore.invalidate();
                } else {
                    oppScore.setVisibility(View.GONE);
                    playerScore.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void handleResignation(final String s, final int i) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boardView.handleResignation(s, i);
            }
        });
    }

    @Override
    public void appendConsole(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                consoleTextView.append(s);
            }

        });
    }

    @Override
    public void newMove(final Board b) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boardView.newMove(b);
            }
        });
    }

    @Override
    public void setPendingOffer(final int i) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boardView.setPendingOffer(i);
            }
        });
    }

    @Override
    public void setPendingOffer(final int i, final int j) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boardView.setPendingOffer(i, j);
            }
        });
    }

    @Override
    public void quit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            this.finishAffinity();
        }
    }
}