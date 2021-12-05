package com.example.mastermind;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.codepath.asynchttpclient.AsyncHttpClient;
import com.codepath.asynchttpclient.RequestParams;
import com.codepath.asynchttpclient.callback.TextHttpResponseHandler;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import okhttp3.Headers;

public class MainActivity extends AppCompatActivity {

    public static final String INTEGER_GENERATOR_API = "https://www.random.org/integers";
    public static final String TAG = "MainActivity";

    private LinearLayout llContainerNumbers;
    private TextView tvGuessBox1;
    private TextView tvGuessBox2;
    private Button btnResetGuess;
    private Button btnSubmitGuess;

    private String[] secretNumber;
    private int[] numbersInSecretNumber;
    private int secretNumberLength;
    private int numberMin;
    private int numberMax;
    private int numberOfGuessesAllowed;
    private int numberOfGuessesUsed;

    private TextView listGuessBoxes[];
    private int currentGuessPosition = 0;
    private PastGuess[] pastGuesses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        llContainerNumbers = findViewById(R.id.llContainerNumbers);

        tvGuessBox1 = findViewById(R.id.tvGuessBox1);
        tvGuessBox2 = findViewById(R.id.tvGuessBox2);
        btnResetGuess = findViewById(R.id.btnResetGuess);
        btnSubmitGuess = findViewById(R.id.btnSubmitGuess);

        secretNumberLength = 2;
        numberMin = 1;
        numberMax = 2;
        numberOfGuessesAllowed = 3;
        numberOfGuessesUsed = 0;
        pastGuesses = new PastGuess[numberOfGuessesAllowed];


        listGuessBoxes = new TextView[secretNumberLength];
        listGuessBoxes[0] = tvGuessBox1;
        listGuessBoxes[1] = tvGuessBox2;

        querySecretNumber();
        createNumberButtons();

        btnSubmitGuess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (remainingGuessesExist()) {
                    if (validGuess()) {
                        submitGuess();
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Invalid guess. Choose a number for each position.", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Toast.makeText(MainActivity.this, "No guesses remaining!", Toast.LENGTH_LONG).show();
                }
            }
        });

        btnResetGuess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetGuessBoxes();
            }
        });
    }

    // Get random secret number from Integer Generator API
    private void querySecretNumber() {

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("num", secretNumberLength);
        params.put("min", numberMin);
        params.put("max", numberMax);
        params.put("col", 1);               // Return response arranged by 1 column per line
        params.put("base", 10);             // Use base 10 number system
        params.put("format", "plain");      // Get return response in plain text
        params.put("rnd", "new");           // Generate new random number

        client.get(INTEGER_GENERATOR_API, params, new TextHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, String response) {
                Log.d(TAG, "Integer Generator API request success!");

                // Store secret number's number value and index location
                secretNumber = response.split("\n");
                Log.i(TAG, "Secret number: " + Arrays.toString(secretNumber));

                // Store secret number's number value
                numbersInSecretNumber = new int[numberMax + 1];
                for (String number : secretNumber) {
                    numbersInSecretNumber[Integer.parseInt(number)] = 1;
                }
                Log.i(TAG, "Numbers in secret number: "
                        + Arrays.toString(numbersInSecretNumber));
            }

            @Override
            public void onFailure(int statusCode, @Nullable Headers headers, String errorResponse,
                                  @Nullable Throwable throwable) {
                Log.d(TAG, "Integer Generator API request failure.");
                // !! Pop up window to notify error and generate new number
            }
        });
    }

    // Create button for each possible number
    private void createNumberButtons() {

        // Set button params
        for (int i = numberMin; i <= numberMax; ++i) {
            final Button button = new Button(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(150, 150);
            params.setMargins(30, 0, 30, 0);
            button.setLayoutParams(params);
            button.setTextSize(24);
            button.setBackgroundColor(Color.CYAN);
            button.setId(i);
            button.setText(Integer.toString(i));
            Log.i(TAG, "Button id: " + button.getId() + ", Button text: " + button.getText());

            // Update current guess box with number clicked
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (currentGuessPosition < secretNumberLength) {
                        TextView currentGuessBox = listGuessBoxes[currentGuessPosition];
                        currentGuessBox.setText(button.getText());
                        ++currentGuessPosition;
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Submit or reset guess",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

            llContainerNumbers.addView(button);
        }
    }

    private Boolean remainingGuessesExist() {
        return numberOfGuessesUsed < numberOfGuessesAllowed;
    }

    // Check if every position in guess has valid number
    private Boolean validGuess() {

        for (int i = 0; i < secretNumberLength; ++i) {
            if (listGuessBoxes[i].getText().toString().equals("?")) {
                return false;
            }
        }

        return true;
    }

    // Submit valid guess
    private void submitGuess() {

        // Add each value from guess box into guess
        String[] guess = new String[secretNumberLength];

        for (int i = 0; i < secretNumberLength; ++i) {
            guess[i] = listGuessBoxes[i].getText().toString();
        }

        /* Record how many value OR value and location
           matches exist between guess and secret number
                1: value match in secret code
                2: value and location match in secret code
           Index does not matter, there is no specific order
        */
        ArrayList<Integer> matchedGuess = new ArrayList<>();

        for (int j = 0; j < secretNumberLength; ++j) {
            if (guess[j].equals(secretNumber[j])
                    && numbersInSecretNumber[Integer.parseInt(guess[j])] == 1) {
                matchedGuess.add(2);
            }
            else if (numbersInSecretNumber[Integer.parseInt(guess[j])] == 1) {
                matchedGuess.add(1);
            }
        }
        Log.i(TAG, "Matched numbers in guess: " + matchedGuess.toString());

        pastGuesses[numberOfGuessesUsed] = new PastGuess(guess, matchedGuess);
        Log.i(TAG, "Guess recorded: " + Arrays.toString(pastGuesses[numberOfGuessesUsed].getGuess()));
        ++numberOfGuessesUsed;

        int numCorrectValueAndLocation = Collections.frequency(matchedGuess, 2);
        int numCorrectValue = Collections.frequency(matchedGuess, 1);

        if (numCorrectValueAndLocation == secretNumberLength) {
            Toast.makeText(MainActivity.this, "Correct!", Toast.LENGTH_LONG).show();
        }
        else if (numCorrectValueAndLocation > 0
                && numCorrectValue > 0) {
            Toast.makeText(MainActivity.this, numCorrectValueAndLocation + " correct value and location + " + numCorrectValue + " correct value", Toast.LENGTH_LONG).show();
        }
        else if (numCorrectValueAndLocation > 0) {
            Toast.makeText(MainActivity.this, numCorrectValueAndLocation + " correct value and location + " , Toast.LENGTH_LONG).show();
        }
        else if (numCorrectValue > 0) {
            Toast.makeText(MainActivity.this, numCorrectValue + " correct value but incorrect location", Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(MainActivity.this, "Wrong numbers!", Toast.LENGTH_LONG).show();
        }

        if (remainingGuessesExist()) {
            resetGuessBoxes();
        }
        else {
            Toast.makeText(MainActivity.this, "Game over!", Toast.LENGTH_LONG).show();
            gameOver();
        }
    }

    // Reset all guess boxes
    private void resetGuessBoxes() {

        for (TextView guessBox : listGuessBoxes) {
            guessBox.setText("?");
        }
        currentGuessPosition = 0;
    }

    private void gameOver() {

    }
}