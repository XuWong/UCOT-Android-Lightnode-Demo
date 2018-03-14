package axu.ln.ucot.ucot.recycler;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import axu.ln.ucot.ucot.R;
import axu.ln.ucot.ucot.utils.FileUtils;
import axu.ln.ucot.ucot.utils.PreferenceUtils;

import java.io.File;

import static axu.ln.ucot.ucot.utils.FileUtils.getName;

final class ViewHolderVideo extends ViewHolder {

    private TextView name;

    private TextView duration;

    ViewHolderVideo(Context context, OnItemClickListener listener, View view) {

        super(context, listener, view);
    }

    @Override
    protected void loadIcon() {

        image = (ImageView) itemView.findViewById(R.id.list_item_image);
    }

    @Override
    protected void loadName() {

        name = (TextView) itemView.findViewById(R.id.list_item_name);
    }

    @Override
    protected void loadInfo() {

        duration = (TextView) itemView.findViewById(R.id.list_item_duration);
    }

    @Override
    protected void bindIcon(File file, Boolean selected) {

        Glide.with(context).load(file).into(image);
    }
    @Override
    protected void bindIconWithState(File file, Boolean selected,int state) {

        Glide.with(context).load(file).into(image);
    }

    @Override
    protected void bindName(File file) {

        boolean extension = PreferenceUtils.getBoolean(context, "pref_extension", true);

        name.setText(extension ? getName(file) : file.getName());
    }

    @Override
    protected void bindInfo(File file) {

        duration.setText(FileUtils.getDuration(file));
    }
}