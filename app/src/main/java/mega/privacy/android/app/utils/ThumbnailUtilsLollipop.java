package mega.privacy.android.app.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video.Thumbnails;
import android.util.TypedValue;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ThumbnailCache;
import mega.privacy.android.app.lollipop.MegaBrowserLollipopAdapter;
import mega.privacy.android.app.lollipop.MegaBrowserLollipopAdapter.ViewHolderBrowser;
import mega.privacy.android.app.lollipop.MegaExplorerLollipopAdapter;
import mega.privacy.android.app.lollipop.MegaExplorerLollipopAdapter.ViewHolderExplorerLollipop;
import mega.privacy.android.app.lollipop.MegaFullScreenImageAdapterLollipop;
import mega.privacy.android.app.lollipop.MegaPhotoSyncGridAdapterLollipop;
import mega.privacy.android.app.lollipop.MegaPhotoSyncGridAdapterLollipop.ViewHolderPhotoSyncGrid;
import mega.privacy.android.app.lollipop.MegaPhotoSyncListAdapterLollipop;
import mega.privacy.android.app.lollipop.MegaPhotoSyncListAdapterLollipop.ViewHolderPhotoSyncList;
import mega.privacy.android.app.lollipop.MegaTransfersLollipopAdapter;
import mega.privacy.android.app.lollipop.MegaTransfersLollipopAdapter.ViewHolderTransfer;
import mega.privacy.android.app.lollipop.providers.MegaProviderLollipopAdapter;
import mega.privacy.android.app.lollipop.providers.MegaProviderLollipopAdapter.ViewHolderLollipopProvider;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUtilsAndroid;


/*
 * Service to create thumbnails
 */
public class ThumbnailUtilsLollipop {
	public static File thumbDir;
	private static int THUMBNAIL_SIZE = 120;
	public static ThumbnailCache thumbnailCache = new ThumbnailCache();
	public static ThumbnailCache thumbnailCachePath = new ThumbnailCache(1);
//	public static ArrayList<Long> pendingThumbnails = new ArrayList<Long>();
	
	static HashMap<Long, ThumbnailDownloadListenerListBrowser> listenersList = new HashMap<Long, ThumbnailDownloadListenerListBrowser>();
//	static HashMap<Long, ThumbnailDownloadListenerGrid> listenersGrid = new HashMap<Long, ThumbnailDownloadListenerGrid>();
//	static HashMap<Long, ThumbnailDownloadListenerNewGrid> listenersNewGrid = new HashMap<Long, ThumbnailDownloadListenerNewGrid>();
	static HashMap<Long, ThumbnailDownloadListenerExplorerLollipop> listenersExplorerLollipop = new HashMap<Long, ThumbnailDownloadListenerExplorerLollipop>();
	static HashMap<Long, ThumbnailDownloadListenerProvider> listenersProvider = new HashMap<Long, ThumbnailDownloadListenerProvider>();
	static HashMap<Long, ThumbnailDownloadListenerFull> listenersFull = new HashMap<Long, ThumbnailDownloadListenerFull>();
	static HashMap<Long, ThumbnailDownloadListenerTransfer> listenersTransfer = new HashMap<Long, ThumbnailDownloadListenerTransfer>();
	static HashMap<Long, ThumbnailDownloadListenerPhotoSyncList> listenersPhotoSyncList = new HashMap<Long, ThumbnailDownloadListenerPhotoSyncList>();
	static HashMap<Long, ThumbnailDownloadListenerPhotoSyncGrid> listenersPhotoSyncGrid = new HashMap<Long, ThumbnailDownloadListenerPhotoSyncGrid>();

	public static Bitmap getRoundedRectBitmap(Context context, final Bitmap bitmap,final int pixels)
	{

		final Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(result);
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);

		final float densityMultiplier = context.getResources().getDisplayMetrics().density;
		final float roundPx = pixels*densityMultiplier;
		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(context.getResources().getColor(R.color.new_background_fragment));
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		//draw rectangles over the corners we want to be square
//		canvas.drawRect(0, 0, bitmap.getWidth()/2, bitmap.getHeight()/2, paint);
//		canvas.drawRect(bitmap.getWidth()/2, 0, bitmap.getWidth(), bitmap.getHeight()/2, paint);

		canvas.drawRect(0, bitmap.getHeight()/2, bitmap.getWidth()/2, bitmap.getHeight(), paint);
		canvas.drawRect(bitmap.getWidth()/2, bitmap.getHeight()/2, bitmap.getWidth(), bitmap.getHeight(), paint);

		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		return result;
	}

	public static Path getRoundedRect(float left, float top, float right, float bottom, float rx, float ry,
								   boolean tl, boolean tr, boolean br, boolean bl){
		Path path = new Path();
		if (rx < 0) rx = 0;
		if (ry < 0) ry = 0;
		float width = right - left;
		float height = bottom - top;
		if (rx > width / 2) rx = width / 2;
		if (ry > height / 2) ry = height / 2;
		float widthMinusCorners = (width - (2 * rx));
		float heightMinusCorners = (height - (2 * ry));

		path.moveTo(right, top + ry);
		if (tr)
			path.rQuadTo(0, -ry, -rx, -ry);//top-right corner
		else{
			path.rLineTo(0, -ry);
			path.rLineTo(-rx,0);
		}
		path.rLineTo(-widthMinusCorners, 0);
		if (tl)
			path.rQuadTo(-rx, 0, -rx, ry); //top-left corner
		else{
			path.rLineTo(-rx, 0);
			path.rLineTo(0,ry);
		}
		path.rLineTo(0, heightMinusCorners);

		if (bl)
			path.rQuadTo(0, ry, rx, ry);//bottom-left corner
		else{
			path.rLineTo(0, ry);
			path.rLineTo(rx,0);
		}

		path.rLineTo(widthMinusCorners, 0);
		if (br)
			path.rQuadTo(rx, 0, rx, -ry); //bottom-right corner
		else{
			path.rLineTo(rx,0);
			path.rLineTo(0, -ry);
		}

		path.rLineTo(0, -heightMinusCorners);

		path.close();//Given close, last lineto can be removed.

		return path;
	}

	
	static class ThumbnailDownloadListenerPhotoSyncList implements MegaRequestListenerInterface{
		Context context;
		ViewHolderPhotoSyncList holder;
		MegaPhotoSyncListAdapterLollipop adapter;
		
		ThumbnailDownloadListenerPhotoSyncList(Context context, ViewHolderPhotoSyncList holder, MegaPhotoSyncListAdapterLollipop adapter){
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		}

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {
			
			log("Downloading thumbnail finished");
			final long handle = request.getNodeHandle();
			MegaNode node = api.getNodeByHandle(handle);
			
//			pendingThumbnails.remove(handle);
			
			if (e.getErrorCode() == MegaError.API_OK){
				log("Downloading thumbnail OK: " + handle);
				thumbnailCache.remove(handle);
				
				if (holder != null){
					File thumbDir = getThumbFolder(context);
					File thumb = new File(thumbDir, node.getBase64Handle()+".jpg");
					if (thumb.exists()) {
						if (thumb.length() > 0) {
							final Bitmap bitmap = getBitmapForCache(thumb, context);
							if (bitmap != null) {
								thumbnailCache.put(handle, bitmap);
								if ((holder.document == handle)){
									holder.imageView.setImageBitmap(bitmap);
									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
									holder.imageView.startAnimation(fadeInAnimation);
									adapter.notifyDataSetChanged();
									log("Thumbnail update");
								}
							}
						}
					}
				}
			}
			else{
				log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}
	}
	
	static class VideoThumbGeneratorListener implements MegaRequestListenerInterface{

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,
				MegaError e) {
			if (e.getErrorCode() == MegaError.API_OK){
				log("OK thumb de video");
			}
			else{
				log("ERROR thumb de video: "+e.getErrorString());
			}
			
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,
				MegaRequest request, MegaError e) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	static class ThumbnailDownloadListenerPhotoSyncGrid implements MegaRequestListenerInterface{
		Context context;
		ViewHolderPhotoSyncGrid holder;
		MegaPhotoSyncGridAdapterLollipop adapter;
		int numView;
		
		ThumbnailDownloadListenerPhotoSyncGrid(Context context, ViewHolderPhotoSyncGrid holder, MegaPhotoSyncGridAdapterLollipop adapter, int numView){
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
			this.numView = numView;
		}

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {
			
			log("Downloading thumbnail finished");
			final long handle = request.getNodeHandle();
			MegaNode node = api.getNodeByHandle(handle);
			
//			pendingThumbnails.remove(handle);
			
			if (e.getErrorCode() == MegaError.API_OK){
				log("Downloading thumbnail OK: " + handle);
				thumbnailCache.remove(handle);
				
				if (holder != null){
					File thumbDir = getThumbFolder(context);
					File thumb = new File(thumbDir, node.getBase64Handle()+".jpg");
					if (thumb.exists()) {
						if (thumb.length() > 0) {
							final Bitmap bitmap = getBitmapForCache(thumb, context);
							if (bitmap != null) {
								thumbnailCache.put(handle, bitmap);
								
								if ((holder.documents.get(numView) == handle)){
									holder.imageViews.get(numView).setImageBitmap(bitmap);
									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
									holder.imageViews.get(numView).startAnimation(fadeInAnimation);
									adapter.notifyDataSetChanged();
									log("Thumbnail update");
								}								
							}
						}
					}
				}
			}
			else{
				log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}
	}
	
	static class ThumbnailDownloadListenerListBrowser implements MegaRequestListenerInterface{
		Context context;
		ViewHolderBrowser holder;
		MegaBrowserLollipopAdapter adapter;
		
		ThumbnailDownloadListenerListBrowser(Context context, ViewHolderBrowser holder, MegaBrowserLollipopAdapter adapter){
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		}

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {
			
			log("Downloading thumbnail finished");
			final long handle = request.getNodeHandle();
			MegaNode node = api.getNodeByHandle(handle);
			
//			pendingThumbnails.remove(handle);
			
			if (e.getErrorCode() == MegaError.API_OK){
				log("Downloading thumbnail OK: " + handle);
				thumbnailCache.remove(handle);
				
				if (holder != null){
					File thumbDir = getThumbFolder(context);
					File thumb = new File(thumbDir, node.getBase64Handle()+".jpg");
					if (thumb.exists()) {
						if (thumb.length() > 0) {
							final Bitmap bitmap = getBitmapForCache(thumb, context);
							if (bitmap != null) {
								thumbnailCache.put(handle, bitmap);
								if ((holder.document == handle)){
									holder.imageView.setImageBitmap(bitmap);
									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
									holder.imageView.startAnimation(fadeInAnimation);
									adapter.notifyDataSetChanged();
									log("Thumbnail update");
								}
							}
						}
					}
				}
			}
			else{
				log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
			}
		}
		
		

		@Override
		public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}
	}

	static class ThumbnailDownloadListenerExplorerLollipop implements MegaRequestListenerInterface{
		Context context;
		ViewHolderExplorerLollipop holder;
		MegaExplorerLollipopAdapter adapter;
		
		ThumbnailDownloadListenerExplorerLollipop(Context context, ViewHolderExplorerLollipop holder, MegaExplorerLollipopAdapter adapter){
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		}

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {
			
			log("Downloading thumbnail finished");
			final long handle = request.getNodeHandle();
			MegaNode node = api.getNodeByHandle(handle);
			
//			pendingThumbnails.remove(handle);
			
			if (e.getErrorCode() == MegaError.API_OK){
				log("Downloading thumbnail OK: " + handle);
				thumbnailCache.remove(handle);
				
				if (holder != null){
					File thumbDir = getThumbFolder(context);
					File thumb = new File(thumbDir, node.getBase64Handle()+".jpg");
					if (thumb.exists()) {
						if (thumb.length() > 0) {
							final Bitmap bitmap = getBitmapForCache(thumb, context);
							if (bitmap != null) {
								thumbnailCache.put(handle, bitmap);
								if ((holder.document == handle)){
									holder.imageView.setImageBitmap(bitmap);
									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
									holder.imageView.startAnimation(fadeInAnimation);
									adapter.notifyDataSetChanged();
									log("Thumbnail update");
								}
							}
						}
					}
				}
			}
			else{
				log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}
	}
	
	static class ThumbnailDownloadListenerProvider implements MegaRequestListenerInterface{
		Context context;
		ViewHolderLollipopProvider holder;
		MegaProviderLollipopAdapter adapter;
		
		ThumbnailDownloadListenerProvider(Context context, ViewHolderLollipopProvider holder, MegaProviderLollipopAdapter adapter){
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		}

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {
			
			log("Downloading thumbnail finished");
			final long handle = request.getNodeHandle();
			MegaNode node = api.getNodeByHandle(handle);
			
//			pendingThumbnails.remove(handle);
			
			if (e.getErrorCode() == MegaError.API_OK){
				log("Downloading thumbnail OK: " + handle);
				thumbnailCache.remove(handle);
				
				if (holder != null){
					File thumbDir = getThumbFolder(context);
					File thumb = new File(thumbDir, node.getBase64Handle()+".jpg");
					if (thumb.exists()) {
						if (thumb.length() > 0) {
							final Bitmap bitmap = getBitmapForCache(thumb, context);
							if (bitmap != null) {
								thumbnailCache.put(handle, bitmap);
								if ((holder.document == handle)){
									holder.imageView.setImageBitmap(bitmap);
									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
									holder.imageView.startAnimation(fadeInAnimation);
									adapter.notifyDataSetChanged();
									log("Thumbnail update");
								}
							}
						}
					}
				}
			}
			else{
				log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}
	}
	
	static class ThumbnailDownloadListenerFull implements MegaRequestListenerInterface{
		Context context;
		MegaFullScreenImageAdapterLollipop.ViewHolderFullImage holder;
		MegaFullScreenImageAdapterLollipop adapter;
		
		ThumbnailDownloadListenerFull(Context context, MegaFullScreenImageAdapterLollipop.ViewHolderFullImage holder, MegaFullScreenImageAdapterLollipop adapter){
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		}

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {
			
			log("Downloading thumbnail finished");
			final long handle = request.getNodeHandle();
			MegaNode node = api.getNodeByHandle(handle);
			
//			pendingThumbnails.remove(handle);
			
			if (e.getErrorCode() == MegaError.API_OK){
				log("Downloading thumbnail OK: " + handle);
				thumbnailCache.remove(handle);
				
				if (holder != null){
					File thumbDir = getThumbFolder(context);
					File thumb = new File(thumbDir, node.getBase64Handle()+".jpg");
					if (thumb.exists()) {
						if (thumb.length() > 0) {
							final Bitmap bitmap = getBitmapForCache(thumb, context);
							if (bitmap != null) {
								thumbnailCache.put(handle, bitmap);
								if ((holder.document == handle)){
									holder.imgDisplay.setImageBitmap(bitmap);
									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
									holder.imgDisplay.startAnimation(fadeInAnimation);
									adapter.notifyDataSetChanged();
									log("Thumbnail update");
								}
							}
						}
					}
				}
			}
			else{
				log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}
	}
	
	static class ThumbnailDownloadListenerTransfer implements MegaRequestListenerInterface{
		Context context;
		ViewHolderTransfer holder;
		MegaTransfersLollipopAdapter adapter;
		
		ThumbnailDownloadListenerTransfer(Context context, ViewHolderTransfer holder, MegaTransfersLollipopAdapter adapter){
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		} 

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {
			
			log("Downloading thumbnail finished");
			final long handle = request.getNodeHandle();
			MegaNode node = api.getNodeByHandle(handle);
			
//			pendingThumbnails.remove(handle);
			
			if (e.getErrorCode() == MegaError.API_OK){
				log("Downloading thumbnail OK: " + handle);
				thumbnailCache.remove(handle);
				
				if (holder != null){
					File thumbDir = getThumbFolder(context);
					File thumb = new File(thumbDir, node.getBase64Handle()+".jpg");
					if (thumb.exists()) {
						if (thumb.length() > 0) {
							final Bitmap bitmap = getBitmapForCache(thumb, context);
							if (bitmap != null) {
								thumbnailCache.put(handle, bitmap);
								if ((holder.document == handle)){
									holder.imageView.setImageBitmap(bitmap);
									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
									holder.imageView.startAnimation(fadeInAnimation);
									adapter.notifyDataSetChanged();
									log("Thumbnail update");
								}
							}
						}
					}
				}
			}
			else{
				log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}
	}
	
	/*
	 * Get thumbnail folder
	 */	
	public static File getThumbFolder(Context context) {
		if (thumbDir == null) {
			if (context.getExternalCacheDir() != null){
				thumbDir = new File (context.getExternalCacheDir(), "thumbnailsMEGA");
			}
			else{
				thumbDir = context.getDir("thumbnailsMEGA", 0);
			}
		}

		if (thumbDir != null){
			thumbDir.mkdirs();
		}

		log("getThumbFolder(): thumbDir= " + thumbDir);
		return thumbDir;
	}
	
	public static Bitmap getThumbnailFromCache(MegaNode node){
		return thumbnailCache.get(node.getHandle());
	}
	
	public static Bitmap getThumbnailFromCache(long handle){
		return thumbnailCache.get(handle);
	}
	
	public static Bitmap getThumbnailFromCache(String path){
		return thumbnailCachePath.get(path);
	}
	
	public static void setThumbnailCache(long handle, Bitmap bitmap){
		thumbnailCache.put(handle, bitmap);
	}
	
	public static void setThumbnailCache(String path, Bitmap bitmap){
		thumbnailCachePath.put(path, bitmap);
	}
	
	public static Bitmap getThumbnailFromFolder(MegaNode node, Context context){
		File thumbDir = getThumbFolder(context);
		File thumb = new File(thumbDir, node.getBase64Handle()+".jpg");
		Bitmap bitmap = null;
		if (thumb.exists()){
			if (thumb.length() > 0){
				bitmap = getBitmapForCache(thumb, context);
				if (bitmap == null) {
					thumb.delete();
				}
				else{
					thumbnailCache.put(node.getHandle(), bitmap);
				}
			}
		}
		return thumbnailCache.get(node.getHandle());

	}
	
	public static Bitmap getThumbnailFromMegaList(MegaNode document, Context context, ViewHolderBrowser viewHolder, MegaApiAndroid megaApi, MegaBrowserLollipopAdapter adapter){
		
//		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
//			log("the thumbnail is already downloaded or added to the list");
//			return thumbnailCache.get(document.getHandle());
//		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
//		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerListBrowser listener = new ThumbnailDownloadListenerListBrowser(context, viewHolder, adapter);
		listenersList.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		log("Lo descargare aqui: " + thumbFile.getAbsolutePath());
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
		
	}
	
	public static Bitmap getThumbnailFromMegaPhotoSyncList(MegaNode document, Context context, ViewHolderPhotoSyncList viewHolder, MegaApiAndroid megaApi, MegaPhotoSyncListAdapterLollipop adapter){
		
//		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
//			log("the thumbnail is already downloaded or added to the list");
//			return thumbnailCache.get(document.getHandle());
//		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
//		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerPhotoSyncList listener = new ThumbnailDownloadListenerPhotoSyncList(context, viewHolder, adapter);
		listenersPhotoSyncList.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		log("Lo descargare aqui: " + thumbFile.getAbsolutePath());
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
		
	}
	
	public static Bitmap getThumbnailFromMegaTransfer(MegaNode document, Context context, ViewHolderTransfer viewHolder, MegaApiAndroid megaApi, MegaTransfersLollipopAdapter adapter){
		
//		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
//			log("the thumbnail is already downloaded or added to the list");
//			return thumbnailCache.get(document.getHandle());
//		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
//		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerTransfer listener = new ThumbnailDownloadListenerTransfer(context, viewHolder, adapter);
		listenersTransfer.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		log("Lo descargare aqui: " + thumbFile.getAbsolutePath());
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
		
	}
	
	public static Bitmap getThumbnailFromMegaExplorerLollipop(MegaNode document, Context context, ViewHolderExplorerLollipop viewHolder, MegaApiAndroid megaApi, MegaExplorerLollipopAdapter adapter){
		
//		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
//			log("the thumbnail is already downloaded or added to the list");
//			return thumbnailCache.get(document.getHandle());
//		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
//		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerExplorerLollipop listener = new ThumbnailDownloadListenerExplorerLollipop(context, viewHolder, adapter);
		listenersExplorerLollipop.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
		
	}
	
	public static Bitmap getThumbnailFromMegaProvider(MegaNode document, Context context, ViewHolderLollipopProvider viewHolder, MegaApiAndroid megaApi, MegaProviderLollipopAdapter adapter){
		
//		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
//			log("the thumbnail is already downloaded or added to the list");
//			return thumbnailCache.get(document.getHandle());
//		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
//		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerProvider listener = new ThumbnailDownloadListenerProvider(context, viewHolder, adapter);
		listenersProvider.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
		
	}

	public static Bitmap getThumbnailFromMegaPhotoSyncGrid(MegaNode document, Context context, ViewHolderPhotoSyncGrid viewHolder, MegaApiAndroid megaApi, MegaPhotoSyncGridAdapterLollipop adapter, int numView){
//		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
//			log("the thumbnail is already downloaded or added to the list");
//			return thumbnailCache.get(document.getHandle());
//		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
//		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerPhotoSyncGrid listener = new ThumbnailDownloadListenerPhotoSyncGrid(context, viewHolder, adapter, numView);
		listenersPhotoSyncGrid.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
	}
	
	public static Bitmap getThumbnailFromMegaFull(MegaNode document, Context context, MegaFullScreenImageAdapterLollipop.ViewHolderFullImage viewHolder, MegaApiAndroid megaApi, MegaFullScreenImageAdapterLollipop adapter){
		
//		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
//			log("the thumbnail is already downloaded or added to the list");
//			return thumbnailCache.get(document.getHandle());
//		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
//		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerFull listener = new ThumbnailDownloadListenerFull(context, viewHolder, adapter);
		listenersFull.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
		
	}
	
	/*
	 * Load Bitmap for cache
	 */
	private static Bitmap getBitmapForCache(File bmpFile, Context context) {
		BitmapFactory.Options bOpts = new BitmapFactory.Options();
		bOpts.inPurgeable = true;
		bOpts.inInputShareable = true;
		Bitmap bmp = BitmapFactory.decodeFile(bmpFile.getAbsolutePath(), bOpts);
		return bmp;
	}
	
	public static class ResizerParams {
		File file;
		MegaNode document;
	}
	
	static class AttachThumbnailTaskExplorerLollipop extends AsyncTask<ResizerParams, Void, Boolean>
	{
		Context context;
		MegaApiAndroid megaApi;
		File thumbFile;
		ResizerParams param;
		ViewHolderExplorerLollipop holder;
		MegaExplorerLollipopAdapter adapter;
		
		AttachThumbnailTaskExplorerLollipop(Context context, MegaApiAndroid megaApi, ViewHolderExplorerLollipop holder, MegaExplorerLollipopAdapter adapter)
		{
			this.context = context;
			this.megaApi = megaApi;
			this.holder = holder;
			this.adapter = adapter;
			this.thumbFile = null;
			this.param = null;
		}
		
		@Override
		protected Boolean doInBackground(ResizerParams... params) {
			log("AttachPreviewStart");
			param = params[0];
			
			File thumbDir = getThumbFolder(context);
			thumbFile = new File(thumbDir, param.document.getBase64Handle()+".jpg");
			boolean thumbCreated = MegaUtilsAndroid.createThumbnail(param.file, thumbFile);

			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			if (shouldContinueObject){
				onThumbnailGeneratedExplorerLollipop(megaApi, thumbFile, param.document, holder, adapter);
			}
		}
	}
	
	static class AttachThumbnailTaskProviderLollipop extends AsyncTask<ResizerParams, Void, Boolean>
	{
		Context context;
		MegaApiAndroid megaApi;
		File thumbFile;
		ResizerParams param;
		ViewHolderLollipopProvider holder;
		MegaProviderLollipopAdapter adapter;
		
		AttachThumbnailTaskProviderLollipop(Context context, MegaApiAndroid megaApi, ViewHolderLollipopProvider holder, MegaProviderLollipopAdapter adapter)
		{
			this.context = context;
			this.megaApi = megaApi;
			this.holder = holder;
			this.adapter = adapter;
			this.thumbFile = null;
			this.param = null;
		}
		
		@Override
		protected Boolean doInBackground(ResizerParams... params) {
			log("AttachPreviewStart");
			param = params[0];
			
			File thumbDir = getThumbFolder(context);
			thumbFile = new File(thumbDir, param.document.getBase64Handle()+".jpg");
			boolean thumbCreated = MegaUtilsAndroid.createThumbnail(param.file, thumbFile);

			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			if (shouldContinueObject){
//				onThumbnailGeneratedExplorerLollipop(megaApi, thumbFile, param.document, holder, adapter);
			}
		}
	}	
	
	private static void onThumbnailGeneratedExplorerLollipop(MegaApiAndroid megaApi, File thumbFile, MegaNode document, ViewHolderExplorerLollipop holder, MegaExplorerLollipopAdapter adapter){
		log("onPreviewGenerated");
		//Tengo que mostrarla
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);
		holder.imageView.setImageBitmap(bitmap);
		thumbnailCache.put(document.getHandle(), bitmap);
		adapter.notifyDataSetChanged();
		
		log("AttachThumbnailTask end");		
	}

	static class AttachThumbnailTaskPhotoSyncList extends AsyncTask<ResizerParams, Void, Boolean>
	{
		Context context;
		MegaApiAndroid megaApi;
		File thumbFile;
		ResizerParams param;
		ViewHolderPhotoSyncList holder;
		MegaPhotoSyncListAdapterLollipop adapter;
		
		AttachThumbnailTaskPhotoSyncList(Context context, MegaApiAndroid megaApi, ViewHolderPhotoSyncList holder, MegaPhotoSyncListAdapterLollipop adapter)
		{
			this.context = context;
			this.megaApi = megaApi;
			this.holder = holder;
			this.adapter = adapter;
			this.thumbFile = null;
			this.param = null;
		}
		
		@Override
		protected Boolean doInBackground(ResizerParams... params) {
			log("AttachPreviewStart");
			param = params[0];
			
			File thumbDir = getThumbFolder(context);
			thumbFile = new File(thumbDir, param.document.getBase64Handle()+".jpg");
			boolean thumbCreated = MegaUtilsAndroid.createThumbnail(param.file, thumbFile);

			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			if (shouldContinueObject){
				onThumbnailGeneratedPhotoSyncList(megaApi, thumbFile, param.document, holder, adapter);
			}
		}
	}
	
	private static void onThumbnailGeneratedPhotoSyncList(MegaApiAndroid megaApi, File thumbFile, MegaNode document, ViewHolderPhotoSyncList holder, MegaPhotoSyncListAdapterLollipop adapter){
		log("onPreviewGenerated");
		//Tengo que mostrarla
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);
		holder.imageView.setImageBitmap(bitmap);
		thumbnailCache.put(document.getHandle(), bitmap);
		adapter.notifyDataSetChanged();
		
		log("AttachThumbnailTask end");		
	}
	
	static class AttachThumbnailTaskList extends AsyncTask<ResizerParams, Void, Boolean>
	{
		Context context;
		MegaApiAndroid megaApi;
		File thumbFile;
		ResizerParams param;
		ViewHolderBrowser holder;
		MegaBrowserLollipopAdapter adapter;
		
		AttachThumbnailTaskList(Context context, MegaApiAndroid megaApi, ViewHolderBrowser holder, MegaBrowserLollipopAdapter adapter)
		{
			this.context = context;
			this.megaApi = megaApi;
			this.holder = holder;
			this.adapter = adapter;
			this.thumbFile = null;
			this.param = null;
		}
		
		@Override
		protected Boolean doInBackground(ResizerParams... params) {
			log("AttachPreviewStart");
			param = params[0];
			
			File thumbDir = getThumbFolder(context);
			thumbFile = new File(thumbDir, param.document.getBase64Handle()+".jpg");
			boolean thumbCreated = MegaUtilsAndroid.createThumbnail(param.file, thumbFile);

			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			if (shouldContinueObject){
				RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
				params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
				params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
				params1.setMargins(18, 0, 12, 0);
				holder.imageView.setLayoutParams(params1);
				
				onThumbnailGeneratedList(megaApi, thumbFile, param.document, holder, adapter);
			}
		}
	}	
		
	private static void onThumbnailGeneratedList(MegaApiAndroid megaApi, File thumbFile, MegaNode document, ViewHolderBrowser holder, MegaBrowserLollipopAdapter adapter){
		log("onPreviewGenerated");
		//Tengo que mostrarla
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);
		holder.imageView.setImageBitmap(bitmap);
		thumbnailCache.put(document.getHandle(), bitmap);
		adapter.notifyDataSetChanged();
		
		log("AttachThumbnailTask end");		
	}
	
	public static void createThumbnailPhotoSyncList(Context context, MegaNode document, ViewHolderPhotoSyncList holder, MegaApiAndroid megaApi, MegaPhotoSyncListAdapterLollipop adapter){
		
		if (!MimeTypeList.typeForName(document.getName()).isImage()) {
			log("no image");
			return;
		}
		
		String localPath = Util.getLocalFile(context, document.getName(), document.getSize(), null); //if file already exists returns != null
		if(localPath != null) //Si la tengo en el sistema de ficheros
		{
			log("localPath no es nulo: " + localPath);
			ResizerParams params = new ResizerParams();
			params.document = document;
			params.file = new File(localPath);
			new AttachThumbnailTaskPhotoSyncList(context, megaApi, holder, adapter).execute(params);
		} //Si no, no hago nada
		
	}
	
	public static void createThumbnailList(Context context, MegaNode document, ViewHolderBrowser holder, MegaApiAndroid megaApi, MegaBrowserLollipopAdapter adapter){
		
		if (!MimeTypeList.typeForName(document.getName()).isImage()) {
			log("no image");
			return;
		}
		
		String localPath = Util.getLocalFile(context, document.getName(), document.getSize(), null); //if file already exists returns != null
		if(localPath != null) //Si la tengo en el sistema de ficheros
		{
			log("localPath no es nulo: " + localPath);
			ResizerParams params = new ResizerParams();
			params.document = document;
			params.file = new File(localPath);
			new AttachThumbnailTaskList(context, megaApi, holder, adapter).execute(params);
		} //Si no, no hago nada
		
	}

	public static void createThumbnailExplorerLollipop(Context context, MegaNode document, ViewHolderExplorerLollipop holder, MegaApiAndroid megaApi, MegaExplorerLollipopAdapter adapter){
		
		if (!MimeTypeList.typeForName(document.getName()).isImage()) {
			log("no image");
			return;
		}
		
		String localPath = Util.getLocalFile(context, document.getName(), document.getSize(), null); //if file already exists returns != null
		if(localPath != null) //Si la tengo en el sistema de ficheros
		{
			log("localPath no es nulo: " + localPath);
			ResizerParams params = new ResizerParams();
			params.document = document;
			params.file = new File(localPath);
			new AttachThumbnailTaskExplorerLollipop(context, megaApi, holder, adapter).execute(params);
		} //Si no, no hago nada
		
	}
	
	public static void createThumbnailProviderLollipop(Context context, MegaNode document, ViewHolderLollipopProvider holder, MegaApiAndroid megaApi, MegaProviderLollipopAdapter adapter){
		
		if (!MimeTypeList.typeForName(document.getName()).isImage()) {
			log("no image");
			return;
		}
		
		String localPath = Util.getLocalFile(context, document.getName(), document.getSize(), null); //if file already exists returns != null
		if(localPath != null) //Si la tengo en el sistema de ficheros
		{
			log("localPath no es nulo: " + localPath);
			ResizerParams params = new ResizerParams();
			params.document = document;
			params.file = new File(localPath);
			new AttachThumbnailTaskProviderLollipop(context, megaApi, holder, adapter).execute(params);
		} //Si no, no hago nada
		
	}
	
	public static void createThumbnailVideo(Context context, String localPath, MegaApiAndroid megaApi, long handle){
		log("createThumbnailVideo: "+localPath+ " : "+handle);
		
		//mp4 and 3gp OK, other formats check from Android DB with loadVideoThumbnail
		// mov, mkv, flv not working even not in Android DB

		MegaNode videoNode = megaApi.getNodeByHandle(handle);
		
		if(videoNode==null){
			log("videoNode is NULL");
			return;
		}
		
		Bitmap bmThumbnail;
		// MICRO_KIND, size: 96 x 96 thumbnail 
		bmThumbnail = ThumbnailUtils.createVideoThumbnail(localPath, Thumbnails.MICRO_KIND);
		if(bmThumbnail==null){
			log("Create video thumb NULL, get with Cursor");
			bmThumbnail= loadVideoThumbnail(localPath, context);
		}	
		else{
			log("Create Video Thumb worked!");
		}
		
		if(bmThumbnail!=null){
			Bitmap resizedBitmap = Bitmap.createScaledBitmap(bmThumbnail, 120, 120, false);		
			
			log("After resize thumb: "+resizedBitmap.getHeight()+" : "+resizedBitmap.getWidth());			
			
			try {
				File thumbDir = getThumbFolder(context);
				File thumbVideo = new File(thumbDir, videoNode.getBase64Handle()+".jpg");
				
				thumbVideo.createNewFile();
				
				FileOutputStream out = null;
				try {
				    out = new FileOutputStream(thumbVideo);
				    boolean result = resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
				    if(result){
				    	log("Compress OK!");
						megaApi.setThumbnail(videoNode, thumbVideo.getAbsolutePath(), new VideoThumbGeneratorListener());
				    }
				    else{
				    	log("Not Compress");
				    }
				} catch (Exception e) {
					log("Error with FileOutputStream: "+e.getMessage());		    
				} finally {
				    try {
				        if (out != null) {
				            out.close();
				        }
				    } catch (IOException e) {
				    	log("Error: "+e.getMessage());
				    }
				}			

			} catch (IOException e1) {
				log("Error creating new thumb file: "+e1.getMessage());	
			}			
		}
		else{
			log("Create video thumb NULL");
		}
	}
	
	private static final String SELECTION = MediaColumns.DATA + "=?";
	private static final String[] PROJECTION = { BaseColumns._ID };
	public static Bitmap loadVideoThumbnail(String videoFilePath,  Context context) {
		log("loadVideoThumbnail");
	    Bitmap result = null;
	    Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
	    String[] selectionArgs = { videoFilePath };
	    ContentResolver cr = context.getContentResolver();
	    Cursor cursor = cr.query(uri, PROJECTION, SELECTION, selectionArgs, null);
	    if (cursor.moveToFirst()) {
	        // it's the only & first thing in projection, so it is 0
	        long videoId = cursor.getLong(0);
	        result = MediaStore.Video.Thumbnails.getThumbnail(cr, videoId, Thumbnails.MICRO_KIND, null);
	    }
	    cursor.close();
	    return result;
	}	
	
	private static void log(String log) {
		Util.log("ThumbnailUtilsLollipop", log);
	}	
}
