package cucumber.android;

import android.content.Context;
import cucumber.resources.AbstractResource;

import java.io.InputStream;

public class AndroidResource extends AbstractResource {
    private Context mContext;
    private int     mResourceId;
    
    public AndroidResource (Context context, int resourceId) {
        super(null);
        mContext    = context;
        mResourceId = resourceId;
    }

    public String getPath () {
        return mContext.getResources().getResourceName(mResourceId);
    }

    public InputStream getInputStream () {
        return mContext.getResources().openRawResource(mResourceId);
    }
}
