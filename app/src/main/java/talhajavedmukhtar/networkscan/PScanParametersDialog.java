package talhajavedmukhtar.networkscan;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Talha on 7/3/18.
 */

public class PScanParametersDialog extends DialogFragment {
    private int noOfIPs;

    private EditText noOfThreads;
    private EditText timeout;

    private TextView estimatedTime;

    public interface ParametersDialogListener{
        void onDialogPositiveClick(DialogFragment dialog);
    }

    ParametersDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View v = inflater.inflate(R.layout.dialog_parameters,null);

        noOfThreads = (EditText) v.findViewById(R.id.noOfThreads);
        timeout = (EditText) v.findViewById(R.id.timeout);

        estimatedTime = (TextView) v.findViewById(R.id.estimatedTime);

        final String estimatedTimeMessage = "Minutes to complete port scan: ";

        noOfIPs = getArguments().getInt("noOfIPs");

        noOfThreads.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(noOfThreads.getText().toString().equals("")){
                    estimatedTime.setText(estimatedTimeMessage + Integer.toString(0));
                }else if(Integer.parseInt(noOfThreads.getText().toString()) == 0){
                    estimatedTime.setText(estimatedTimeMessage + Integer.toString(0));
                }else{
                    int minsToComplete = calculateEstTime();
                    estimatedTime.setText(estimatedTimeMessage + Integer.toString(minsToComplete));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        timeout.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(timeout.getText().toString().equals("")){
                    estimatedTime.setText(estimatedTimeMessage + Integer.toString(0));
                }else{
                    int minsToComplete = calculateEstTime();
                    estimatedTime.setText(estimatedTimeMessage + Integer.toString(minsToComplete));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(v)
                // Add action buttons
                .setPositiveButton("PROCEED", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if(Integer.parseInt(noOfThreads.getText().toString()) == 0){
                            Toast.makeText(getActivity(),"Cannot start with 0 threads",Toast.LENGTH_SHORT).show();
                        }else if(Integer.parseInt(timeout.getText().toString()) == 0){
                            Toast.makeText(getActivity(),"Cannot start with 0 timeout",Toast.LENGTH_SHORT).show();
                        } else{
                            mListener.onDialogPositiveClick(PScanParametersDialog.this);
                        }
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        PScanParametersDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (ParametersDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    public int getNoOfThreads(){
        return Integer.parseInt(noOfThreads.getText().toString());
    }

    public int getTimeout(){
        return Integer.parseInt(timeout.getText().toString());
    }

    public int calculateEstTime(){
        //in minutes
        int threads = Integer.parseInt(noOfThreads.getText().toString());
        int tO = Integer.parseInt(timeout.getText().toString());
        int newEstTime = (65536/threads)*tO*noOfIPs;
        return newEstTime/60;
    }
}
