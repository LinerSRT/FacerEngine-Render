package com.liner.facerengineview;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import java.util.List;

public class SkinChooseAdapter extends Adapter<SkinChooseAdapter.ViewHolder> {
	private Context context;
	private List<SkinHolder> clockItemsList;

	private int selectedPos = -1;

	public SkinChooseAdapter(Context context,List<SkinHolder> clockItemsList) {
		this.context = context;
		this.clockItemsList = clockItemsList;
	}

	@Override
	public int getItemCount() {
		return clockItemsList.size();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_skin_choose, parent, false);
		return new ViewHolder(v);
	}


	@Override
	public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
		selectedPos = position;
		final SkinHolder skin = clockItemsList.get(position);
		holder.clockModel.setImageBitmap(skin.getPreview());
		holder.clockModel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(context, WatchFaceViewActivity.class);
				intent.putExtra("skinpath", skin.getFilePath());
				context.startActivity(intent);
			}
		});
	}

	class ViewHolder extends RecyclerView.ViewHolder{
		ImageView clockModel;
		ViewHolder(final View view) {
			super(view);
			clockModel = view.findViewById(R.id.skinPreview);
		}
	}

}
