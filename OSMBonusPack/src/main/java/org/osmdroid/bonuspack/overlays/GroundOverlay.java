package org.osmdroid.bonuspack.overlays;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.OverlayWithIW;

/**
 * A ground overlay is an image that is fixed to a map. 
 * Mimics the GroundOverlay class from Google Maps Android API v2 as much as possible. Main differences:<br/>
 * - Doesn't support: Z-Index<br/>
 * - image is a standard Android BitmapDrawable, instead of the BitmapDescriptor introduced in Maps API. <br/>
 * - supports onSingleTapConfirmed event
 * - TODO: supports InfoWindow
 * 
 * @author M.Kergall
 * @see <a href="http://developer.android.com/reference/com/google/android/gms/maps/model/GroundOverlay.html">Google Maps GroundOverlay</a>
 *
 */
public class GroundOverlay extends OverlayWithIW {

	protected Drawable mImage;
	protected GeoPoint mPosition;
	protected float mBearing;
	protected float mWidth, mHeight;
	protected float mTransparency;
	public final static float NO_DIMENSION = -1.0f;
	protected Point mPositionPixels, mSouthEastPixels;
	protected OnClickListener mOnClickListener;

	public GroundOverlay() {
		super();
		mWidth = 10.0f;
		mHeight = NO_DIMENSION;
		mBearing = 0.0f;
		mTransparency = 0.0f;
		mPositionPixels = new Point();
		mSouthEastPixels = new Point();
	}

	public void setImage(Drawable image){
		mImage = image;
	}
	
	public Drawable getImage(){
		return mImage;
	}
	
	public GeoPoint getPosition(){
		return mPosition.clone();
	}
	
	public void setPosition(GeoPoint position){
		mPosition = position.clone();
	}

	public float getBearing(){
		return mBearing;
	}
	
	public void setBearing(float bearing){
		mBearing = bearing;
	}
	
	public void setDimensions(float width){
		mWidth = width;
		mHeight = NO_DIMENSION;
	}
	
	public void setDimensions(float width, float height){
		mWidth = width;
		mHeight = height;
	}

	public float getHeight(){
		return mHeight;
	}
	
	public float getWidth(){
		return mWidth;
	}
	
	public void setTransparency(float transparency){
		mTransparency = transparency;
	}
	
	public float getTransparency(){
		return mTransparency;
	}

	protected void computeHeight(){
		if (mHeight == NO_DIMENSION && mImage != null){
			mHeight = mWidth * mImage.getIntrinsicHeight() / mImage.getIntrinsicWidth();
		}
	}

	/** @return the bounding box, ignoring the bearing of the GroundOverlay (similar to Google Maps API) */
	public BoundingBox getBoundingBox(){
		computeHeight();
		GeoPoint pEast = mPosition.destinationPoint(mWidth, 90.0f);
		GeoPoint pSouthEast = pEast.destinationPoint(mHeight, -180.0f);
		double north = mPosition.getLatitude()*2 - pSouthEast.getLatitude();
		double west = mPosition.getLongitude()*2 - pEast.getLongitude();
		return new BoundingBox(north, pEast.getLongitude(), pSouthEast.getLatitude(), west);
	}
	
	public void setPositionFromBounds(BoundingBox bb){
		mPosition = bb.getCenterWithDateLine();
		GeoPoint pEast = new GeoPoint(mPosition.getLatitude(), bb.getLonEast());
		GeoPoint pWest = new GeoPoint(mPosition.getLatitude(), bb.getLonWest());
		mWidth = (float)pEast.distanceToAsDouble(pWest);
		GeoPoint pSouth = new GeoPoint(bb.getLatSouth(), mPosition.getLongitude());
		GeoPoint pNorth = new GeoPoint(bb.getLatNorth(), mPosition.getLongitude());
		mHeight = (float)pSouth.distanceToAsDouble(pNorth);
	}

	@Override public void draw(Canvas canvas, Projection pj) {
		if (mImage == null)
			return;

		computeHeight();

		pj.toPixels(mPosition, mPositionPixels);
		GeoPoint pEast = mPosition.destinationPoint(mWidth/2, 90.0f);
		GeoPoint pSouthEast = pEast.destinationPoint(mHeight/2, -180.0f);
		pj.toPixels(pSouthEast, mSouthEastPixels);
		int hWidth = mSouthEastPixels.x-mPositionPixels.x;
		int hHeight = mSouthEastPixels.y-mPositionPixels.y;
		mImage.setBounds(-hWidth, -hHeight, hWidth, hHeight);

		mImage.setAlpha(255-(int)(mTransparency*255));

		drawAt(canvas, mImage, mPositionPixels.x, mPositionPixels.y, false, -mBearing);
	}

	public boolean contains(GeoPoint p){
		BoundingBox bb = getBoundingBox();
		return bb.contains(p);
		//TODO handle bearing.
	}

	@Override public boolean onSingleTapConfirmed(final MotionEvent event, final MapView mapView) {
		final Projection pj = mapView.getProjection();
		GeoPoint eventPos = (GeoPoint) pj.fromPixels((int) event.getX(), (int) event.getY());
		boolean touched = contains(eventPos);
		if (touched) {
			if (mOnClickListener == null) {
				return onClickDefault(this, eventPos);
			} else {
				return mOnClickListener.onClick(this, eventPos);
			}
		} else
			return touched;
	}

	//-- Events listener interfaces ------------------------------------

	public interface OnClickListener {
		abstract boolean onClick(GroundOverlay groundOverlay, GeoPoint eventPos);
	}

	/**
	 * default behaviour when no click listener is set
	 */
	public boolean onClickDefault(GroundOverlay groundOverlay, GeoPoint eventPos) {
		/*
		groundOverlay.setInfoWindowLocation(eventPos);
		groundOverlay.showInfoWindow();
		*/
		//By default, do nothing
		return false;
	}

	public void setOnClickListener(OnClickListener listener) {
		mOnClickListener = listener;
	}

}
