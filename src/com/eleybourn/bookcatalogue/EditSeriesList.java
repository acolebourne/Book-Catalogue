/*
 * @copyright 2011 Philip Warner
 * @license GNU General Public License
 * 
 * This file is part of Book Catalogue.
 *
 * Book Catalogue is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Book Catalogue is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Book Catalogue.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.eleybourn.bookcatalogue;

import java.util.ArrayList;

import com.eleybourn.bookcatalogue.Fields.Field;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class EditSeriesList extends EditObjectList<Series> {

	private ArrayAdapter<String> mSeriesAdapter;

	public EditSeriesList() {
		super(CatalogueDBAdapter.KEY_SERIES_ARRAY, R.layout.edit_series_list, R.layout.row_edit_series_list);
	}

	@Override
	protected void onSetupView(View target, Series object) {
        if (object != null) {
	        TextView dt = (TextView) target.findViewById(R.id.row_series);
	        if (dt != null)
	              dt.setText(object.getDisplayName());

	        TextView st = (TextView) target.findViewById(R.id.row_series_sort);
	        if (st != null) {
		        if (object.getDisplayName().equals(object.getSortName())) {
		        	st.setVisibility(View.GONE);
		        } else {
		        	st.setVisibility(View.VISIBLE);
					st.setText(object.getSortName());
	        	}
        	}
			EditText et = (EditText) target.findViewById(R.id.row_series_num);
			if (et != null)
				et.setText(object.num);
        }
	}

	protected ArrayList<String> getSeriesFromDb() {
		ArrayList<String> series_list = new ArrayList<String>();
		Cursor series_cur = mDbHelper.fetchAllSeries();
		startManagingCursor(series_cur);
		while (series_cur.moveToNext()) {
			String series = series_cur.getString(series_cur.getColumnIndexOrThrow(CatalogueDBAdapter.KEY_SERIES_NAME));
			series_list.add(series);
		}
		series_cur.close();
		return series_list;
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {

			mSeriesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, getSeriesFromDb());
			((AutoCompleteTextView)this.findViewById(R.id.series)).setAdapter(mSeriesAdapter);
	
		} catch (Exception e) {
			Log.e("BookCatalogue.EditSeriesList.onCreate","Failed to initialize", e);
		}
	}

	@Override
	protected void onAdd(View v) {
		AutoCompleteTextView t = ((AutoCompleteTextView)EditSeriesList.this.findViewById(R.id.series));
		String s = t.getText().toString().trim();
		if (s.length() > 0) {
			EditText et = ((EditText)EditSeriesList.this.findViewById(R.id.series_num));
			String n = et.getText().toString();
			if (n == null)
				n = "";
			Series series = new Series(t.getText().toString(), n);
			series.id = mDbHelper.lookupSeriesId(series);
			boolean foundMatch = false;
			for(int i = 0; i < mList.size() && !foundMatch; i++) {
				if (series.id != 0L) {
					if (mList.get(i).id == series.id)
						foundMatch = true;
				} else {
					if (series.name.equals(mList.get(i).name))
						foundMatch = true;
				}
			}
			if (foundMatch) {
				Toast.makeText(EditSeriesList.this, getResources().getString(R.string.series_already_in_list), Toast.LENGTH_LONG).show();						
				return;							
			}
			mList.add(series);
            mAdapter.notifyDataSetChanged();
		} else {
			Toast.makeText(EditSeriesList.this, getResources().getString(R.string.series_is_blank), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onRowClick(View target, final Series object) {
		editSeries(object);
	}
	
	private void editSeries(final Series series) {
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.edit_book_series);
		dialog.setTitle(R.string.edit_book_series);

		AutoCompleteTextView seriesView = (AutoCompleteTextView) dialog.findViewById(R.id.series);
		seriesView.setText(series.name);
		seriesView.setAdapter(mSeriesAdapter);

		EditText numView = (EditText) dialog.findViewById(R.id.series_num);
		numView.setText(series.num);

		setTextOrHideView(dialog.findViewById(R.id.title_label), mBookTitleLabel);
		setTextOrHideView(dialog.findViewById(R.id.title), mBookTitle);

		Button saveButton = (Button) dialog.findViewById(R.id.confirm);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AutoCompleteTextView seriesView = (AutoCompleteTextView) dialog.findViewById(R.id.series);
				EditText numView = (EditText) dialog.findViewById(R.id.series_num);
				String newName = seriesView.getText().toString().trim();
				if (newName == null || newName.length() == 0) {
					Toast.makeText(EditSeriesList.this, R.string.series_is_blank, Toast.LENGTH_LONG).show();
					return;
				}
				Series newSeries = new Series(newName, numView.getText().toString());
				confirmEditSeries(series, newSeries);
				dialog.dismiss();
			}
		});
		Button cancelButton = (Button) dialog.findViewById(R.id.cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		
		dialog.show();		
	}

	private void confirmEditSeries(final Series oldSeries, final Series newSeries) {
		// First, deal with a some special cases...
		
		boolean nameIsSame = (newSeries.name.compareTo(oldSeries.name) == 0);
		// Case: Unchanged.
		if (nameIsSame && newSeries.num.compareTo(oldSeries.num) == 0) {
			// No change to anything; nothing to do
			return;
		}
		if (nameIsSame) {
			// Same name, different number... just update
			oldSeries.copyFrom(newSeries);
			Utils.pruneList(mDbHelper, mList);
			mAdapter.notifyDataSetChanged();
			return;
		}

		// Get the new IDs
		oldSeries.id = mDbHelper.lookupSeriesId(oldSeries);
		newSeries.id = mDbHelper.lookupSeriesId(newSeries);

		// See if the old series is used in any other books.
		long nRefs = mDbHelper.getSeriesBookCount(oldSeries);
		boolean oldHasOthers = nRefs > (mRowId == 0 ? 0 : 1);

		// Case: series is the same (but different case), or is only used in this book
		if (newSeries.id == oldSeries.id || !oldHasOthers) {
			// Just update with the most recent spelling and format
			oldSeries.copyFrom(newSeries);
			Utils.pruneList(mDbHelper, mList);
			mDbHelper.sendSeries(oldSeries);
			mAdapter.notifyDataSetChanged();
			return;
		}

		// TODO Stringify this procedure!
		// When we get here, we know the names are genuinely different and the old series is used in more than one place.
		final AlertDialog alertDialog = new AlertDialog.Builder(this).setMessage("You have changed the series from:\n  '" 
										+ oldSeries.name + "' to \n  '" + newSeries.name 
										+ "'\nHow do you wish to apply this change? "
										+ "\nNote: The choice 'All Books' will be applied instantly.").create();

		alertDialog.setTitle("Scope of Change");
		alertDialog.setIcon(android.R.drawable.ic_menu_info_details);
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "This Book", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				oldSeries.copyFrom(newSeries);
				Utils.pruneList(mDbHelper, mList);
				mAdapter.notifyDataSetChanged();
				alertDialog.dismiss();
			}
		});

		alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "All Books", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mDbHelper.globalReplaceSeries(oldSeries, newSeries);
				oldSeries.copyFrom(newSeries);
				Utils.pruneList(mDbHelper, mList);
				mAdapter.notifyDataSetChanged();
				alertDialog.dismiss();
			}
		}); 

		alertDialog.show();
	}
}
