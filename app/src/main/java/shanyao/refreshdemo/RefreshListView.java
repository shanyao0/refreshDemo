package shanyao.refreshdemo;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.ListView;

import butterknife.Bind;
import butterknife.ButterKnife;


public class RefreshListView extends ListView {

    private static final int PULLDOWN_STATE = 0;// 下拉刷新
    private static final int RELEASE_STATE = 1;// 松开刷新
    private static final int REFRESHING_STATE = 2;// 正在刷新
    @Bind(R.id.iv_1)
    ImageView iv1;
    @Bind(R.id.iv_2)
    ImageView iv2;
    @Bind(R.id.iv_3)
    ImageView iv3;
    private int current_state = PULLDOWN_STATE;// 当前状态，默认是下拉刷新
    private int downY = -1;
    private float headerHeight;
    private View header;

    private OnRefreshingListener mListener;
    private ScaleAnimation bigScale;
    private ScaleAnimation smallScale;
    private AnimationDrawable frame;

    public RefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initHeader();
        initAnimation();
    }

    /**
     * 添加动画效果
     */
    private void initAnimation() {
        smallScale = new ScaleAnimation(1.0f, 0.5f, 1.0f, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        smallScale.setDuration(500);
        frame = (AnimationDrawable) iv2.getBackground();
    }

    /**
     * 初始化头布局
     */
    private void initHeader() {
        header = View.inflate(getContext(), R.layout.refresh_header, null);
        ButterKnife.bind(this,header);
        // 添加头布局
        this.addHeaderView(header);
        // 隐藏头布局
        header.measure(0, 0);
        headerHeight = header.getMeasuredHeight();
        header.setPadding(0, (int) -headerHeight, 0, 0);
    }

    /**
     * 处理触摸事件
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downY = (int) ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                // 如果当前显示的不是第一个条目就不加刷新
                if (getFirstVisiblePosition() != 0) {
                    break;
                }
                int moveY = (int) ev.getY();
                // 在第一个图片的条目上down事件没有
                if (getFirstVisiblePosition() == 0 && downY == -1) {
                    downY = moveY;
                }
                float diffY = moveY - downY;
                if (diffY > 0) {
                    float topPadding = diffY - headerHeight;
                    if (topPadding < 0 && current_state != PULLDOWN_STATE) {
                        current_state = PULLDOWN_STATE;
                        changeState(current_state);
                    }
                    if (topPadding > 0 && current_state != RELEASE_STATE) {
                        current_state = RELEASE_STATE;
                        changeState(current_state);
                    }
                    header.setPadding(0, (int) topPadding, 0, 0);
                    float scale =  diffY/headerHeight;
                    if(scale>0.4&&scale<1){
                        Log.e("zs","scale"+scale+"---topPadding"+-topPadding+"---headerHeight"+headerHeight);
                        iv1.setScaleX(scale);
                        iv1.setScaleY(scale);
                        iv1.setVisibility(VISIBLE);
                    }

                    return true;// 自己消费事件
                }
                break;
            case MotionEvent.ACTION_UP:
                downY = -1;
                // 手指松开时，根据当前状态判断是否切换到正在刷新
                if (current_state == PULLDOWN_STATE) {
                    // 直接隐藏头布局
                    header.setPadding(0, (int) -headerHeight, 0, 0);
                } else if (current_state == RELEASE_STATE) {
                    current_state = REFRESHING_STATE;
                    changeState(current_state);
                    System.out.println("切换到正在刷新");
                    // 让头布局弹到正好完全展示
                    header.setPadding(0, 0, 0, 0);
                    // 调用外界监听器的具体实现方法
                    if (mListener != null) {
                        mListener.onRefreshing();
                    }
                }
                break;

            default:
                break;
        }
        return super.onTouchEvent(ev);
    }

    /**
     * 根据当前的状态设置更新头的不同状态
     */
    private void changeState(int current_state) {
        switch (current_state) {
            case PULLDOWN_STATE:
                iv2.setVisibility(INVISIBLE);
                iv3.setVisibility(INVISIBLE);
                break;
            case RELEASE_STATE:
                break;
            case REFRESHING_STATE:
                frame.start();
                iv1.setVisibility(INVISIBLE);
                iv2.setVisibility(VISIBLE);
                iv3.setVisibility(INVISIBLE);
                break;
            default:
                break;
        }
    }

    /**
     * 定义下拉刷新接口，让外界实现具体的业务
     */
    public interface OnRefreshingListener {
        // 下拉刷新方法
        void onRefreshing();
    }

    /**
     * 向外界提供设置监听器的方法
     */
    public void setOnRefreshingListener(OnRefreshingListener listener) {
        this.mListener = listener;
    }

    /**
     * 刷新后刷新头消失
     */
    public void refreshingFinished() {

        iv3.startAnimation(smallScale);
        iv1.setVisibility(INVISIBLE);
        iv2.setVisibility(INVISIBLE);
        iv3.setVisibility(VISIBLE);
        new CountDownTimer(500,10){

            @Override
            public void onTick(long millisUntilFinished) {
                float scale  =1f- millisUntilFinished/500f;
                header.setPadding(0, (int) ((int) -headerHeight*scale), 0, 0);
            }

            @Override
            public void onFinish() {
                header.setPadding(0, (int) -headerHeight, 0, 0);
            }
        }.start();
    }
}
