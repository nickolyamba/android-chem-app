package app.android.chemicals;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity implements View.OnClickListener {


    private String USER_ID = "";
    private String TOKEN = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // define button objects
        final Button suppButton = (Button) findViewById(R.id.supplier_button);
        suppButton.setOnClickListener(this);
        final Button chemButton = (Button) findViewById(R.id.chem_button);
        chemButton.setOnClickListener(this);
        final Button solutButton = (Button) findViewById(R.id.solution_button);
        solutButton.setOnClickListener(this);
        final Button exitButton = (Button) findViewById(R.id.exit_button);
        exitButton.setOnClickListener(this);

        Intent intent = getIntent();

        TOKEN = intent.getStringExtra("TOKEN");
        USER_ID = intent.getStringExtra("USER_ID");
    }

    private static final String LOM = "LOM";
    public void onClick(View v) {
        // Create an explicit Intent for starting the HelloAndroid
        // Activity
        switch (v.getId())
        {
            case R.id.supplier_button:
                Log.d(LOM, "Supplier button clicked");
                // Create an explicit Intent for starting the HelloAndroid
                // Activity
                // http://startandroid.ru/en/lessons/complete-list/
                // 241-lesson-28-extras-passing-data-using-intent.html
                Intent supplierIntent = new Intent(this, SupplierActivity.class);
                supplierIntent.putExtra("Token", TOKEN);
                supplierIntent.putExtra("USER_ID", USER_ID);
                // Use the Intent to start the SupplierFragment Activity
                startActivity(supplierIntent);
                break;
            case R.id.chem_button:
                Log.d(LOM, "Chem button clicked");
                Intent chemIntent = new Intent(this, ChemicalActivity.class);
                chemIntent.putExtra("Token", TOKEN);
                chemIntent.putExtra("USER_ID", USER_ID);
                //chemIntent.putExtra("lname", etLName.getText().toString());
                startActivity(chemIntent);
                break;
            case R.id.solution_button:
                Log.d(LOM, "Solution button clicked");
                break;
            case R.id.exit_button:
                Log.d(LOM, "Exit button clicked");
                //this.onBackPressed();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
                break;
        }//switch
    }//onClick()
}//MainActivity


