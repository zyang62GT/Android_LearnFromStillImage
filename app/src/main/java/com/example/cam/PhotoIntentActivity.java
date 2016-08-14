package com.example.cam;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jetpac.deepbelief.DeepBelief;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cam.R;







public class PhotoIntentActivity extends Activity implements  View.OnTouchListener {

	private DragController mDragController;   // Object that sends out drag-drop events while a view is being moved.
	private DragLayer mDragLayer;             // The ViewGroup that supports drag-drop.
	// Otherwise, any touch event starts a drag.

	private static final int CHANGE_TOUCH_MODE_MENU_ID = Menu.FIRST;

	public static final boolean Debugging = false;


	private static final int ACTION_TAKE_PHOTO_B = 1;
	private static final int ACTION_TAKE_PHOTO_S = 2;
	private static final int ACTION_SELECT_GALLERY = 3;

	private static final String BITMAP_STORAGE_KEY = "viewbitmap";
	private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
	private ImageView i1;
	private TextView i2;
	private TextView i3;
	private TextView tb;
	private int leftOff = 0;
	private int topOff = 0;
	private Bitmap mImageBitmap;
	String picturePath ="";
	RelativeLayout relativeLayout;


	private String mCurrentPhotoPath;

	private static final String JPEG_FILE_PREFIX = "IMG_";
	private static final String JPEG_FILE_SUFFIX = ".jpg";

	private AlbumStorageDirFactory mAlbumStorageDirFactory = null;

	String path = Environment.getExternalStorageDirectory().toString()+"/Documents/newFile";
	//Log.d("Files", "Path: " + path);
	File f = new File(path);
	File file[] = f.listFiles();


	TextView labelsView;

	Activity act;
	Context ctx;

	//Pointer varaibles for jpcnn api
	Pointer networkHandle = null;
	Pointer[] predictors = new Pointer[10];


	float[] preVals = new float[10];
	String labelsText = "";

	
	/* Photo album for this application */
	private String getAlbumName() {
		return getString(R.string.album_name);
	}

	
	private File getAlbumDir() {
		File storageDir = null;

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			
			storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());

			if (storageDir != null) {
				if (! storageDir.mkdirs()) {
					if (! storageDir.exists()){
						Log.d("CameraSample", "failed to create directory");
						return null;
					}
				}
			}
			
		} else {
			Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
		}
		
		return storageDir;
	}

	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
		File albumF = getAlbumDir();
		File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
		return imageF;
	}

	private File setUpPhotoFile() throws IOException {
		
		File f = createImageFile();
		mCurrentPhotoPath = f.getAbsolutePath();
		
		return f;
	}

	private void setPic() {

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
		int targetW = i1.getWidth();
		int targetH = i1.getHeight();

		/* Get the size of the image */
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;
		
		/* Figure out which way needs to be reduced less */
		int scaleFactor = 1;
		if ((targetW > 0) || (targetH > 0)) {
			scaleFactor = Math.min(photoW / targetW, photoH / targetH);
		}

		/* Set bitmap options to scale the image decode target */
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
		Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		
		/* Associate the Bitmap to the ImageView */
		i1.setImageBitmap(bitmap);
		//process predictions here
		classifyBitmap(bitmap);
		i1.setVisibility(View.VISIBLE);
	}

	void initDeepBelief() {
		AssetManager am = ctx.getAssets();
		String baseFileName = "jetpac.ntwk";
		String dataDir = ctx.getFilesDir().getAbsolutePath();
		String networkFile = dataDir + "/" + baseFileName;
		copyAsset(am, baseFileName, networkFile);
		networkHandle = DeepBelief.JPCNNLibrary.INSTANCE.jpcnn_create_network(networkFile);

	}

	private static boolean copyAsset(AssetManager assetManager,
									 String fromAssetPath, String toPath) {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = assetManager.open(fromAssetPath);
			new File(toPath).createNewFile();
			out = new FileOutputStream(toPath);
			copyFile(in, out);
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private static void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while((read = in.read(buffer)) != -1){
			out.write(buffer, 0, read);
		}
	}

	void classifyBitmap(Bitmap bitmap) {
		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();
		final int pixelCount = (width * height);
		final int bytesPerPixel = 4;
		final int byteCount = (pixelCount * bytesPerPixel);
		ByteBuffer buffer = ByteBuffer.allocate(byteCount);
		bitmap.copyPixelsToBuffer(buffer);
		byte[] pixels = buffer.array();
		Pointer imageHandle = DeepBelief.JPCNNLibrary.INSTANCE.jpcnn_create_image_buffer_from_uint8_data(pixels, width, height, 4, (4 * width), 0, 0);

		PointerByReference predictionsValuesRef = new PointerByReference();
		IntByReference predictionsLengthRef = new IntByReference();
		PointerByReference predictionsNamesRef = new PointerByReference();
		IntByReference predictionsNamesLengthRef = new IntByReference();
		long startT = System.currentTimeMillis();
		DeepBelief.JPCNNLibrary.INSTANCE.jpcnn_classify_image(
				networkHandle,
				imageHandle,
				2, //SAMPLE FLAGS: 0 = DEFAULT(CENTERED), 1 = MULTISAMPLE, 2 = RANDOM_SAMPLE
				-2, //LAYEROFFSET
				predictionsValuesRef,
				predictionsLengthRef,
				predictionsNamesRef,
				predictionsNamesLengthRef);
		long stopT = System.currentTimeMillis();
		float duration = (float)(stopT-startT) / 1000.0f;
		System.err.println("jpcnn_classify_image() took " + duration + " seconds.");

		DeepBelief.JPCNNLibrary.INSTANCE.jpcnn_destroy_image_buffer(imageHandle);

		Pointer predictionsValuesPointer = predictionsValuesRef.getValue();
		final int predictionsLength = predictionsLengthRef.getValue();
		//Pointer predictionsNamesPointer = predictionsNamesRef.getValue();
		//final int predictionsNamesLength = predictionsNamesLengthRef.getValue();

		System.err.println(String.format("predictionsLength = %d", predictionsLength));

		float[] predictionsValues = predictionsValuesPointer.getFloatArray(0, predictionsLength);
		//Pointer[] predictionsNames = predictionsNamesPointer.getPointerArray(0);

		//Send predictions to predictionHandler
		predictionHandler(predictionsValuesPointer, predictionsLength);
		//PredictionLabel label = new PredictionLabel(file[0].getName().toString(),preVal);
		//labelsText = String.format("%s - %.2f\n",label.name, label.predictionValue);
		//labelsText += String.format("%s - %.2f\n",label2.name, label2.predictionValue);
		labelsText = "";
		//set labelsText

		/*for(int i=0;i<10;i++){
			if(i>=file.length) break;
			PredictionLabel label = new PredictionLabel(file[i].getName().toString(),preVals[i]);

			labelsText += String.format("%s - %.2f\n",label.name, label.predictionValue);
		}*/
		labelsView.setText(labelsText);
	}

	private class PredictionLabel implements Comparable<PredictionLabel> {
		public String name;
		public float predictionValue;
		public PredictionLabel(String inName, float inPredictionValue) {
			this.name = inName;
			this.predictionValue = inPredictionValue;
		}
		public int compareTo(PredictionLabel anotherInstance) {
			final float diff = (this.predictionValue - anotherInstance.predictionValue);
			if (diff < 0.0f) {
				return 1;
			} else if (diff > 0.0f) {
				return -1;
			} else {
				return 0;
			}
		}
	};

	public void startPre(){


		for(int i=0;i<10;i++){
			if(i>=file.length) break;
			predictors[i] = DeepBelief.JPCNNLibrary.INSTANCE.jpcnn_load_predictor(path + '/' + file[i].getName().toString());
		}

	}

	public void predictionHandler (Pointer predictions,int predictionsLength){

		for(int i=0;i<10;i++){
			if(i>=file.length) break;
			preVals[i] = DeepBelief.JPCNNLibrary.INSTANCE.jpcnn_predict(predictors[i],predictions,predictionsLength);
		}

	}

	private void galleryAddPic() {
		    Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
			File f = new File(mCurrentPhotoPath);
		    Uri contentUri = Uri.fromFile(f);
		    mediaScanIntent.setData(contentUri);
		    this.sendBroadcast(mediaScanIntent);
	}

	private void dispatchTakePictureIntent(int actionCode) {

		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		switch(actionCode) {
		case ACTION_TAKE_PHOTO_B:
			File f = null;
			
			try {
				f = setUpPhotoFile();
				mCurrentPhotoPath = f.getAbsolutePath();
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
			} catch (IOException e) {
				e.printStackTrace();
				f = null;
				mCurrentPhotoPath = null;
			}
			break;

		default:
			break;			
		} // switch

		startActivityForResult(takePictureIntent, actionCode);
	}



	private void handleSmallCameraPhoto(Intent intent) {
		Bundle extras = intent.getExtras();
		mImageBitmap = (Bitmap) extras.get("data");
		i1.setImageBitmap(mImageBitmap);
		i1.setVisibility(View.VISIBLE);
	}

	private void handleBigCameraPhoto() {

		if (mCurrentPhotoPath != null) {
			setPic();
			galleryAddPic();
			mCurrentPhotoPath = null;
		}

	}

	private void handleCameraVideo(Intent intent) {
		mImageBitmap = null;
		i1.setVisibility(View.INVISIBLE);
	}

	Button.OnClickListener mTakePicOnClickListener =
		new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
		}
	};





	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_photopre);
		mDragController = new DragController(this);

		i1 = (ImageView) findViewById(R.id.imageView1);
		mImageBitmap = null;


		Button picBtn = (Button) findViewById(R.id.btnIntend);
		setBtnListenerOrDisable(
				picBtn,
				mTakePicOnClickListener,
				MediaStore.ACTION_IMAGE_CAPTURE
		);




		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
		} else {
			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
		}
		ctx = this;
		act = this;




		labelsView = (TextView) findViewById(R.id.labelsView);
		labelsView.setText("");

		//TextView textView = new TextView(ctx);
		//textView.setBackgroundResource(R.drawable.bg_rectangle_with_stroke_dash);
		//relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout);

		//relativeLayout.addView(textView);

		setupViews ();



		initDeepBelief();
		startPre();
	}

	public void galleryClick(View view){
		Intent intent=new Intent();
		intent.setAction(Intent.ACTION_PICK);//Pick an item from the data
		intent.setType("image/*");//select from all pictures
		startActivityForResult(intent, ACTION_SELECT_GALLERY);

	}

	public void xyClick(View view){
		String text="";
		text+=" i1.Left:";
		text+=i1.getLeft();
		text+=" i1.Top:";
		text+=i1.getTop();
		text+=" i2.Left:";
		text+=i2.getLeft();
		text+=" i2.Top:";
		text+=i2.getTop();
		text+=" i3.Left:";
		text+=i3.getLeft();
		text+=" i3.Top:";
		text+=i3.getTop();
		leftOff = i2.getLeft();
		topOff = i2.getTop();
		tb.setText(text);

	}

	public void cropClick(View view){


	}

	public void addWidthClick(View view){
		ViewGroup.LayoutParams params2 = i2.getLayoutParams();
		//Button new width
		params2.width += 10;

		i2.setLayoutParams(params2);
		ViewGroup.LayoutParams params1 = i3.getLayoutParams();
		//Button new width
		params1.width += 10;

		i3.setLayoutParams(params1);


	}
	public void addHeightClick(View view){
		ViewGroup.LayoutParams params2 = i2.getLayoutParams();
		//Button new width
		params2.height += 10;

		i2.setLayoutParams(params2);
		ViewGroup.LayoutParams params1 = i3.getLayoutParams();
		//Button new width
		params1.height += 10;

		i3.setLayoutParams(params1);

	}
	public void reduceWidthClick(View view){
		ViewGroup.LayoutParams params2 = i2.getLayoutParams();
		//Button new width
		params2.width -= 10;

		i2.setLayoutParams(params2);
		ViewGroup.LayoutParams params1 = i3.getLayoutParams();
		//Button new width
		params1.width -= 10;

		i3.setLayoutParams(params1);


	}
	public void reduceHeightClick(View view){
		ViewGroup.LayoutParams params2 = i2.getLayoutParams();
		//Button new width
		params2.height -= 10;

		i2.setLayoutParams(params2);
		ViewGroup.LayoutParams params1 = i3.getLayoutParams();
		//Button new width
		params1.height -= 10;

		i3.setLayoutParams(params1);

	}



	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case ACTION_TAKE_PHOTO_B: {
				if (resultCode == RESULT_OK) {
					handleBigCameraPhoto();
				}
				break;
			} // ACTION_TAKE_PHOTO_B

			case ACTION_TAKE_PHOTO_S: {
				if (resultCode == RESULT_OK) {
					handleSmallCameraPhoto(data);
				}
				break;
			} // ACTION_TAKE_PHOTO_S


			case ACTION_SELECT_GALLERY:{
				if (resultCode==RESULT_OK) {//select oroginal photo from album
					try {
						Uri selectedImage = data.getData(); //get the uri returned by system
						String[] filePathColumn = { MediaStore.Images.Media.DATA };
						Cursor cursor = getContentResolver().query(selectedImage,
								filePathColumn, null, null, null);//look for the certain photo in system
						cursor.moveToFirst();
						int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
						picturePath = cursor.getString(columnIndex);  //get photo path
						cursor.close();
						mImageBitmap= BitmapFactory.decodeFile(picturePath);
						i1.setImageBitmap(mImageBitmap);
						classifyBitmap(mImageBitmap);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				break;
			}

		} // switch
	}

	// Some lifecycle callbacks so that the image can survive orientation change
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
		outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (mImageBitmap != null));
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
		i1.setImageBitmap(mImageBitmap);
		i1.setVisibility(
				savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY) ? 
						ImageView.VISIBLE : ImageView.INVISIBLE
		);

	}

	/**
	 * Indicates whether the specified action can be used as an intent. This
	 * method queries the package manager for installed packages that can
	 * respond to an intent with the specified action. If no suitable package is
	 * found, this method returns false.
	 * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
	 *
	 * @param context The application's environment.
	 * @param action The Intent action to check for availability.
	 *
	 * @return True if an Intent with the specified action can be sent and
	 *         responded to, false otherwise.
	 */
	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list =
			packageManager.queryIntentActivities(intent,
					PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	private void setBtnListenerOrDisable( 
			Button btn,
			Button.OnClickListener onClickListener,
			String intentName
	) {
		if (isIntentAvailable(this, intentName)) {
			btn.setOnClickListener(onClickListener);        	
		} else {
			btn.setText( 
				getText(R.string.cannot).toString() + " " + btn.getText());
			btn.setClickable(false);
		}
	}

	public boolean onCreateOptionsMenu (Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		menu.add(0, CHANGE_TOUCH_MODE_MENU_ID, 0, "Change Touch Mode");
		return true;
	}

	/**
	 * Handle a click on a view. Tell the user to use a long click (press).
	 *
	 */



	/**
	 * Handle a click on the Wglxy views at the bottom.
	 *
	 */

	public void onClickWglxy (View v) {
		Intent viewIntent = new Intent ("android.intent.action.VIEW",
				Uri.parse("http://double-star.appspot.com/blahti/ds-download.html"));
		startActivity(viewIntent);

	}





	/**
	 * Perform an action in response to a menu item being clicked.
	 *
	 */


	/**
	 * Resume the activity.
	 */

	@Override protected void onResume() {
		super.onResume();

		View v  = findViewById (R.id.wglxy_bar);
		if (v != null) {
			Animation anim1 = AnimationUtils.loadAnimation(this, R.anim.fade_in);
			//anim1.setAnimationListener (new StartActivityAfterAnimation (i));
			v.startAnimation (anim1);
		}
	}

	/**
	 * This is the starting point for a drag operation if mLongClickStartsDrag is false.
	 * It looks for the down event that gets generated when a user touches the screen.
	 * Only that initiates the drag-drop sequence.
	 *
	 */

	public boolean onTouch (View v, MotionEvent ev)
	{
		// If we are configured to start only on a long click, we are not going to handle any events here.

		boolean handledHere = false;

		final int action = ev.getAction();

		// In the situation where a long click is not needed to initiate a drag, simply start on the down event.
		if (action == MotionEvent.ACTION_DOWN) {
			handledHere = startDrag (v);
			if (handledHere) v.performClick ();
		}

		return handledHere;
	}

	/**
	 * Start dragging a view.
	 *
	 */

	public boolean startDrag (View v)
	{
		// Let the DragController initiate a drag-drop sequence.
		// I use the dragInfo to pass along the object being dragged.
		// I'm not sure how the Launcher designers do this.
		Object dragInfo = v;
		mDragController.startDrag (v, mDragLayer, dragInfo, DragController.DRAG_ACTION_MOVE);
		return true;
	}

	/**
	 * Finds all the views we need and configure them to send click events to the activity.
	 *
	 */
	private void setupViews()
	{
		DragController dragController = mDragController;

		mDragLayer = (DragLayer) findViewById(R.id.drag_layer);
		mDragLayer.setDragController(dragController);
		dragController.addDropTarget(mDragLayer);

		i2 = (TextView) findViewById (R.id.Text1);
		i2.setOnTouchListener(this);

		i3 = (TextView) findViewById (R.id.Text2);
		i3.setOnTouchListener(this);


		i1 = (ImageView) findViewById (R.id.imageView1);
		//i1.setOnTouchListener(this);

		tb = (TextView) findViewById(R.id.Textbot);









	}

	/**
	 * Show a string on the screen via Toast.
	 *
	 * @param msg String
	 * @return void
	 */

	public void toast (String msg)
	{
		Toast.makeText (getApplicationContext(), msg, Toast.LENGTH_SHORT).show ();
	} // end toast

	/**
	 * Send a message to the debug log and display it using Toast.
	 */

	public void trace (String msg)
	{
		if (!Debugging) return;
		Log.d ("PhotopreActivity", msg);
		toast (msg);
	}

}