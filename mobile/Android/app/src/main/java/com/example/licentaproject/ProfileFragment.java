package com.example.licentaproject;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.licentaproject.models.User;
import com.example.licentaproject.utils.HttpRequestUtil;
import com.example.licentaproject.utils.SessionData;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment implements View.OnClickListener {


    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        EditText firstNameField = view.findViewById(R.id.firstNameField);
        EditText lastNameField = view.findViewById(R.id.lastNameField);
        EditText emailField = view.findViewById(R.id.emailField);
        Button updateBtn = view.findViewById(R.id.updateBtn);

        User user = SessionData.getUser();
        firstNameField.setText(user.getFirstName());
        lastNameField.setText(user.getLastName());
        emailField.setText(user.getEmail());

        updateBtn.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        String firstName = ((EditText) getActivity().findViewById(R.id.firstNameField)).getText().toString();
        String lastName = ((EditText) getActivity().findViewById(R.id.lastNameField)).getText().toString();
        String email = ((EditText) getActivity().findViewById(R.id.emailField)).getText().toString();
        String password = ((EditText) getActivity().findViewById(R.id.passwordField)).getText().toString();
        String passwordConfirm = ((EditText) getActivity().findViewById(R.id.passwordConfirmField)).getText().toString();

        User user = SessionData.getUser();
        if (!firstName.isEmpty()) {
            user.setFirstName(firstName);
        } else {
            Toast.makeText(getContext(), "Please enter your first name", Toast.LENGTH_LONG).show();
            return;
        }

        if (!lastName.isEmpty()) {
            user.setLastName(lastName);
        } else {
            Toast.makeText(getContext(), "Please enter your last name", Toast.LENGTH_LONG).show();
            return;
        }

        if (!email.isEmpty() && Pattern.compile("\\S+@\\S+\\.\\S+").matcher(email).matches()) {
            user.setEmail(email);
        } else {
            Toast.makeText(getContext(), "Please enter a valid e-mail address", Toast.LENGTH_LONG).show();
            return;
        }

        if (!password.isEmpty()) {
            if (password.equals(passwordConfirm)) {
                user.setPassword(password);
            } else {
                Toast.makeText(getContext(), "Please enter a valid password", Toast.LENGTH_LONG).show();
                return;
            }
        }

        new UpdateUserTask(getContext()).execute(user);
    }

    private class UpdateUserTask extends AsyncTask<User, Void, Map> {

        private Context context;

        public UpdateUserTask(Context context) {
            this.context = context;
        }

        @Override
        protected Map doInBackground(User... params) {
            return (Map) HttpRequestUtil.sendRequest("resource/me/user", "PUT", params[0], Map.class, false);
        }

        protected void onPostExecute(Map status) {
            if (status == null) {
                Log.d("USER_UPDATE_STATUS", "Update failed!");
                Toast.makeText(context, "User update failed, please try again!", Toast.LENGTH_LONG).show();
            } else {
                Log.d("USER_UPDATE_STATUS", "Update successful");
            }
        }
    }

}
