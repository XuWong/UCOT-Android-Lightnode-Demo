package axu.ln.ucot.ucot.recycler;

import android.content.Context;
import android.support.v7.util.SortedList;
import android.util.SparseBooleanArray;

import java.io.File;

/**
 * Created by Aluminum on 2018/3/14.
 */

public class VerifiedFile {
    public File f;
//    0 is unsecured; 1 is verified; 2 is failed to be verified
    public int state;
    public String lcMD5;
    public String bcMD5;

    public VerifiedFile(File f,int state,String lcMD5,String bcMD5) {

        this.f = f;
        this.state = state;
        this.lcMD5 = lcMD5;
        this.bcMD5 = bcMD5;
    }
}
