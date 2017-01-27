package cc.cu.altarus.rss;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Created by travis on 12/30/16.
 */
public class RssFeedListAdapter extends RecyclerView.Adapter<RssFeedListAdapter.FeedModelViewHolder> {
    private List<RssFeedModel> mRssFeedModels;
    //private ImageGetter imageGetter;

    public static class FeedModelViewHolder extends RecyclerView.ViewHolder {
        private View rssFeedView;

        public FeedModelViewHolder(View v) {
            super(v);
            rssFeedView = v;
        }
    }

    public RssFeedListAdapter(List<RssFeedModel> rssFeedModels) {
        mRssFeedModels = rssFeedModels;
    }

    @Override
    public FeedModelViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rss_feed, parent, false);
        FeedModelViewHolder holder = new FeedModelViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(FeedModelViewHolder holder, int position) {
        final RssFeedModel rssFeedModel = mRssFeedModels.get(position);
        ((TextView) holder.rssFeedView.findViewById(R.id.titleText)).setText(rssFeedModel.title);
        TextView descrTextView = (TextView) holder.rssFeedView.findViewById(R.id.descriptionText);
        ImageView imageView = (ImageView) holder.rssFeedView.findViewById(R.id.imageView);
        ImageLoader imageLoader = new ImageLoader(imageView);
        imageLoader.execute(rssFeedModel.imgSrc);
        SpannableString spannableDescr = new SpannableString(Html.fromHtml(rssFeedModel.description));
        ImageSpan[] imgSpans = spannableDescr.getSpans(0, spannableDescr.length(), ImageSpan.class);
        //remove all embedded image placeholders
        for (ImageSpan is : imgSpans) {
            spannableDescr.removeSpan(is);
        }
        descrTextView.setText(spannableDescr);
        descrTextView.setCompoundDrawables(null, null, null, null);
        ((TextView) holder.rssFeedView.findViewById(R.id.linkText)).setText(rssFeedModel.link);
    }

    @Override
    public int getItemCount() {
        return mRssFeedModels.size();
    }

    public class ImageLoader extends AsyncTask<String, Void, Bitmap> {

        private final WeakReference<ImageView> mWeakImageView;

        public ImageLoader(ImageView im) {
            mWeakImageView = new WeakReference<>(im);
        }

        @Override
        protected Bitmap doInBackground(String... urlAddrs) {
            URL url = null;
            String urlAddr = urlAddrs[0];
            if(urlAddrs == null || urlAddr == null){
                return null;
            }
            try {
                Log.v("ImageLoader", "Parsed URL is: " + urlAddr);
                url = new URL(urlAddr);
                InputStream inputStream = url.openConnection().getInputStream();
                return BitmapFactory.decodeStream(inputStream);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                ImageView imageView = mWeakImageView.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }

    }
}
