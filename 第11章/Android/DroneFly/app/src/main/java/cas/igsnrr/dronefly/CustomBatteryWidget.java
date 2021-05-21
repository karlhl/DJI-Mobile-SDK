package cas.igsnrr.dronefly;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Toast;

import dji.ux.widget.BatteryWidget;

public class CustomBatteryWidget extends BatteryWidget {

    public CustomBatteryWidget(Context context) {
        super(context);
    }

    public CustomBatteryWidget(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public CustomBatteryWidget(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override
    public void initView(Context context, AttributeSet attributeSet, int i) {
        super.initView(context, attributeSet, i);
    }

    @Override
    public void onBatteryPercentageChange(int i) {
        Toast.makeText(context, "电池电量变化:" + i + "%", Toast.LENGTH_LONG).show();
    }
}
