package com.bluesoft.android;

import java.util.ArrayList;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView; 
import android.widget.Toast;

public class DialogLoad extends DialogFragment
{
	String mPresetName;
	
	public DialogLoad()
	{
		super();

		mPresetName = "";
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.dialog_load, container, false);

		ListView filesCtrl = (ListView)v.findViewById(R.id.presets);

		filesCtrl.setClickable(true);
		filesCtrl.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				mPresetName = ((TextView)view).getText().toString();
				BSTicker activity = (BSTicker) getActivity();
				activity.onFinishDialogLoad(DialogLoad.this);
				DialogLoad.this.getDialog().dismiss();
			}
		});

		ArrayList<String> presetFiles = ((BSTicker)getActivity()).getPresetNames();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, presetFiles);
		filesCtrl.setAdapter(adapter);

		// watch for button clicks.
		final Button btnBack = (Button)v.findViewById(R.id.dialogButtonBack);
		btnBack.setOnClickListener(new OnClickListener() { public void onClick(View v) { DialogLoad.this.getDialog().cancel(); } });

		return v;
	}
}

