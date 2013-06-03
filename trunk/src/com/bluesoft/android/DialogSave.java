package com.bluesoft.android;

import android.app.Dialog;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class DialogSave extends DialogFragment
{
	String mPresetName;
	
	public DialogSave()
	{
		super();

		mPresetName = "Preset name";
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.dialog_save, container, false);

		final EditText ctrlPresetName = (EditText)v.findViewById(R.id.preset);
		ctrlPresetName.setText(mPresetName);

		// watch for button clicks.
		final Button btnOk = (Button)v.findViewById(R.id.dialogButtonOK);
		btnOk.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				// update pattern and exit
				mPresetName = ctrlPresetName.getText().toString();
				BSTicker activity = (BSTicker) getActivity();
				activity.onFinishDialogSave(DialogSave.this);
				DialogSave.this.getDialog().dismiss();
			}
			//Toast.makeText(getActivity(), "Size must number > 0", Toast.LENGTH_LONG).show();
		});

		// Watch for cancel button clicks.
		final Button btnCancel = (Button)v.findViewById(R.id.dialogButtonCancel);
		btnCancel.setOnClickListener(new OnClickListener() { public void onClick(View v) { DialogSave.this.getDialog().cancel(); } });

		return v;
	}
}

