package cc.cu.altarus.rss

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.MalformedURLException
import java.net.URL

class RssFeedListAdapter(
    private val mRssFeedModels: List<RssFeedModel>
) : RecyclerView.Adapter<RssFeedListAdapter.FeedModelViewHolder>() {

    class FeedModelViewHolder(val rssFeedView: View) : RecyclerView.ViewHolder(rssFeedView) {
        val titleText: TextView = rssFeedView.findViewById(R.id.titleText) as TextView
        val descriptionText: TextView = rssFeedView.findViewById(R.id.descriptionText) as TextView
        val imageView: ImageView = rssFeedView.findViewById(R.id.imageView) as ImageView
        val linkText: TextView = rssFeedView.findViewById(R.id.linkText) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): FeedModelViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rss_feed, parent, false)
        return FeedModelViewHolder(v)
    }

    override fun onBindViewHolder(holder: FeedModelViewHolder, position: Int) {
        val rssFeedModel = mRssFeedModels[position]
        holder.titleText.text = rssFeedModel.title ?: ""
        holder.linkText.text = rssFeedModel.link ?: ""

        val rawDescription = rssFeedModel.description ?: ""
        @Suppress("DEPRECATION")
        val spanned = Html.fromHtml(rawDescription)

        val spannableDescr = SpannableString(spanned)
        val imgSpans = spannableDescr.getSpans(0, spannableDescr.length, ImageSpan::class.java)
        // Remove all embedded image placeholders
        for (span in imgSpans) {
            spannableDescr.removeSpan(span)
        }

        holder.descriptionText.text = spannableDescr
        holder.descriptionText.setCompoundDrawables(null, null, null, null)

        holder.imageView.setImageDrawable(null)
        val imgSrc = rssFeedModel.imgSrc
        if (!imgSrc.isNullOrEmpty()) {
            holder.imageView.tag = imgSrc
            ImageLoader(holder.imageView, imgSrc).execute(imgSrc)
        } else {
            holder.imageView.tag = null
        }
    }

    override fun getItemCount(): Int = mRssFeedModels.size

    class ImageLoader(
        imageView: ImageView,
        private val expectedUrl: String?
    ) : AsyncTask<String, Void, Bitmap?>() {

        private val mWeakImageView: WeakReference<ImageView> = WeakReference(imageView)

        override fun doInBackground(vararg urlAddrs: String?): Bitmap? {
            if (urlAddrs.isEmpty()) return null
            val urlAddr = urlAddrs[0] ?: return null

            return try {
                Log.v(TAG, "Parsed URL is: $urlAddr")
                val url = URL(urlAddr)
                url.openConnection().getInputStream().use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: MalformedURLException) {
                Log.e(TAG, "Malformed URL: $urlAddr", e)
                null
            } catch (e: IOException) {
                Log.e(TAG, "IO error downloading image from: $urlAddr", e)
                null
            }
        }

        override fun onPostExecute(bitmap: Bitmap?) {
            if (bitmap != null) {
                val imageView = mWeakImageView.get()
                if (imageView != null && imageView.tag == expectedUrl) {
                    imageView.setImageBitmap(bitmap)
                }
            }
        }

        companion object {
            private const val TAG = "ImageLoader"
        }
    }
}
