package com.example.dragtoactionlayoutdemo.view


import android.content.Context
import android.support.v4.view.MotionEventCompat
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.Transformation
import android.widget.AbsListView
import android.widget.RelativeLayout
import android.widget.TextView
import com.example.dragtoactionlayoutdemo.R
import com.example.dragtoactionlayoutdemo.utils.DensityUtil

/**
 * Created by rebecca on 17/6/19.
 * 左滑查看更多控件
 */
class DragToActionLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : RelativeLayout(context, attrs) {
    /**
     * 滑动模式
     */
    private var mDragMode = LIMIT_DRAG_DISTANCE
    /**
     * 内容view
     */
    private var mTarget: View? = null

    /**
     * 滑动查看跟多view
     */
    private val mRefreshView: RelativeLayout?
    private val mDecelerateInterpolator: Interpolator
    private val mTouchSlop: Int
    /**
     * 触发刷新的滑动距离，也是mRefreshView刷新时停留的高度
     */
    var totalDragDistance: Int = 0
        private set
    /**
     * 滑动进度
     */
    private var mCurrentDragPercent: Float = 0.toFloat()
    /**
     * mCurrentOffsetRight=mTarget.getTop();内容view的top值
     */
    private var mCurrentOffsetRight: Int = 0

    private var mActivePointerId: Int = 0
    /**
     * 是否被滑动
     */
    private var mIsBeingDragged: Boolean = false
    /**
     * 滑动初始x坐标
     */
    private var mInitialMotionX: Float = 0.toFloat()
    private var mInitialMotionY: Float = 0.toFloat()
    private var mFrom: Int = 0
    private var mFromDragPercent: Float = 0.toFloat()
    /**
     * 滑动监听
     */
    private var mListener: OnDragListener? = null
    /**
     * 是否在回弹动画结束后执行响应事件
     */
    private val isAnimFinishDoAction = true
    /**
     * 是否在响应滑到有效距离的事件
     */
    private var isAction: Boolean = false
    private var textView: TextView? = null

    init {

        mDecelerateInterpolator = DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR)
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        totalDragDistance = DensityUtil.dp2px(context, DRAG_MAX_DISTANCE.toFloat())

        mRefreshView = RelativeLayout(context)

        addView(mRefreshView)

        initRefreshView()

        //在构造函数上加上这句，防止自定义View的onDraw方法不执行的问题
        setWillNotDraw(false)
        ViewCompat.setChildrenDrawingOrderEnabled(this, true)
    }

    private fun initRefreshView() {
        val headerView = setRefreshView(R.layout.view_drag)
        textView = headerView!!.findViewById(R.id.text_view) as TextView
        textView!!.text = "左滑查看更多"
    }

    fun setRefreshView(layoutId: Int): View? {
        if (mRefreshView == null) {
            return null
        }
        mRefreshView.removeAllViews()
        val layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.MATCH_PARENT)
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1)
        mRefreshView.layoutParams = layoutParams
        return LayoutInflater.from(context).inflate(layoutId, mRefreshView, true)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        ensureTarget()

        if (mTarget == null)
            return

        widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(measuredWidth - paddingRight - paddingLeft, View.MeasureSpec.AT_MOST)
        heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(measuredHeight - paddingTop - paddingBottom, View.MeasureSpec.AT_MOST)
        mTarget!!.measure(widthMeasureSpec, heightMeasureSpec)
        mRefreshView!!.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), heightMeasureSpec)


        totalDragDistance = (mRefreshView.measuredWidth * 0.8f).toInt()
    }

    /**
     * 获取内容view
     */
    private fun ensureTarget() {
        if (mTarget != null)
            return
        if (childCount > 0) {
            for (i in 0..childCount - 1) {
                val child = getChildAt(i)
                if (child !== mRefreshView) {
                    mTarget = child

                }
            }
        }
    }

    /**
     * 初步判断是否自己处理事件
     * 如果可用、到达最右端、未正在响应查看更多事件时
     * 初步判断需要自己处理事件
     */
    private fun isHandleEventBySelf():Boolean{
        return isEnabled && !canChildScrollRight() &&!isAction
    }
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if(isAction){
            //滑动开始响应查看更多事件时，屏蔽父控件事件
            parent.requestDisallowInterceptTouchEvent(true)
        }else {
            //水平滑动屏蔽父控件
            val action = MotionEventCompat.getActionMasked(ev)

            when (action) {
            //手指按下，记录点击坐标
                MotionEvent.ACTION_DOWN -> {
                    //                setTargetOffsetRight(0, true);
                    mActivePointerId = MotionEventCompat.getPointerId(ev, 0)
                    mIsBeingDragged = false
                    val initialMotionX = getMotionEventX(ev, mActivePointerId)
                    if (initialMotionX == -1f) {
                        return false
                    }
                    mInitialMotionX = initialMotionX
                    mInitialMotionY = getMotionEventY(ev, mActivePointerId)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mActivePointerId == INVALID_POINTER) {
                        return false
                    }
                    val x = getMotionEventX(ev, mActivePointerId)
                    val y = getMotionEventY(ev, mActivePointerId)
                    if (x == -1f) {
                        return false
                    }
                    val xDiff = mInitialMotionX - x
                    val yDiff = mInitialMotionY - y
                    //如果是横向滑动屏蔽父控件事件
                    if (xDiff > mTouchSlop && xDiff > yDiff) {
                        //屏蔽父控件事件（viewPager），解决ViewPager中子View的滑动冲突问题
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                }

            }
        }
        return super.dispatchTouchEvent(ev)
    }
    /**
     * 该函数只干两件事
     * 1.记录手指按下的坐标
     * 2.判断是否拦截处理事件
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {

        //初步判断自己不处理事件时，不拦截点击事件，不做任何处理
        if (!isHandleEventBySelf()) {
            return false
        }
        val action = MotionEventCompat.getActionMasked(ev)

        when (action) {
        //手指按下，记录点击坐标
            MotionEvent.ACTION_DOWN -> {
                //                setTargetOffsetRight(0, true);
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0)
                mIsBeingDragged = false
                val initialMotionX = getMotionEventX(ev, mActivePointerId)
                if (initialMotionX == -1f) {
                    return false
                }
                mInitialMotionX = initialMotionX
            }
            MotionEvent.ACTION_MOVE -> {
                if (mActivePointerId == INVALID_POINTER) {
                    return false
                }
                val x = getMotionEventX(ev, mActivePointerId)
                if (x == -1f) {
                    return false
                }
                val xDiff = mInitialMotionX - x
                //如果是滑动动作，将标志mIsBeingDragged置为true
                if (xDiff > mTouchSlop && !mIsBeingDragged) {
                    mIsBeingDragged = true
                }
            }
        //手指松开，标志复位
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mIsBeingDragged = false
                mActivePointerId = INVALID_POINTER
            }
            MotionEventCompat.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
        }

        //进一步判断是否拦截事件，如果是正在被滑动拖动，拦截该事件，不往下传递；反之，你懂的
        return mIsBeingDragged
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {

        //如果不是在被滑动拖动，不处理，直接返回
        if (!mIsBeingDragged) {
            return super.onTouchEvent(ev)
        }

        val action = MotionEventCompat.getActionMasked(ev)

        when (action) {
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId)
                if (pointerIndex < 0) {
                    return false
                }

                //将要移到的位置对应的X坐标，也是在未松手前，target滑动的总距离 int
                val targetX: Int
                val x = MotionEventCompat.getX(ev, pointerIndex)
                val xDiff = mInitialMotionX - x
                //未松手前,总共滑动的距离 float
                val scrollRight = xDiff * DRAG_RATE
                if (mCurrentDragPercent < 0) {
                    return false
                }
                if (mDragMode == LIMIT_DRAG_DISTANCE) {
                    //控制滑动的最大距离
                    mCurrentDragPercent = scrollRight / totalDragDistance
                    val boundedDragPercent = Math.min(1f, Math.abs(mCurrentDragPercent))
                    val extraOS = Math.abs(scrollRight) - totalDragDistance
                    val slingshotDist = totalDragDistance.toFloat()
                    val tensionSlingshotPercent = Math.max(0f,
                            Math.min(extraOS, slingshotDist * 2) / slingshotDist)
                    val tensionPercent = (tensionSlingshotPercent / 4 - Math.pow(
                            (tensionSlingshotPercent / 4).toDouble(), 2.0)).toFloat() * 2f
                    val extraMove = slingshotDist * tensionPercent / 2
                    targetX = (slingshotDist * boundedDragPercent + extraMove).toInt()
                } else {
                    targetX = scrollRight.toInt() //效果一样,但可以无限滑动
                }
                if (mListener != null && !isAction) {
                    if (mCurrentDragPercent >= 1.0f) {
                        textView!!.text = "松开查看更多"
                    } else {
                        textView!!.text = "左滑查看更多"
                    }
                    mListener!!.onDragDistanceChange(scrollRight, mCurrentDragPercent, (scrollRight - mCurrentOffsetRight) / totalDragDistance)
                }

                mRefreshView!!.visibility = View.VISIBLE
                //调整更新位置，传过去的值是每次的偏移量
                setTargetOffsetRight(targetX - mCurrentOffsetRight, true)
            }
        //做多指触控处理
            MotionEventCompat.ACTION_POINTER_DOWN -> {
                //将最后一只按下的手指作为ActivePointer
                val index = MotionEventCompat.getActionIndex(ev)
                mActivePointerId = MotionEventCompat.getPointerId(ev, index)
            }
            MotionEventCompat.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
        //手指松开！
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                //排除是无关手指
                if (mActivePointerId == INVALID_POINTER) {
                    return false
                }
                val pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId)
                val x = MotionEventCompat.getX(ev, pointerIndex)
                //计算松开瞬间滑动的距离
                val overScrollRight = (mInitialMotionX - x) * DRAG_RATE
                //标志复位
                mIsBeingDragged = false
                mActivePointerId = INVALID_POINTER

                if (overScrollRight > totalDragDistance) {
                    //滑动到有效距离，提示松手响应事件
                    onAction()
                } else {//回滚
                    isAction = false
                    animateOffsetToStartPosition()
                }
                return false//系列点击事件已经处理完，将处理权交还mTarget
            }
        }

        return true//该系列点击事件未处理完，消耗此系列事件
    }

    /**
     * 回滚动画
     */
    private fun animateOffsetToStartPosition() {
        mFrom = mCurrentOffsetRight
        mFromDragPercent = mCurrentDragPercent
        val animationDuration = Math.abs((MAX_OFFSET_ANIMATION_DURATION * mFromDragPercent).toLong())
        mAnimateToStartPosition.reset()
        mAnimateToStartPosition.duration = animationDuration
        mAnimateToStartPosition.interpolator = mDecelerateInterpolator
        mAnimateToStartPosition.setAnimationListener(mToStartListener)
        mRefreshView!!.clearAnimation()
        mRefreshView.startAnimation(mAnimateToStartPosition)
    }


    private val mAnimateToStartPosition = object : Animation() {
        public override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            moveToStart(interpolatedTime)
        }
    }


    private fun moveToStart(interpolatedTime: Float) {
        val targetRight = mFrom - (mFrom * interpolatedTime).toInt()
        val targetPercent = mFromDragPercent * (1.0f - interpolatedTime)
        //计算偏移量
        val offset = targetRight - (mTarget!!.measuredWidth - mTarget!!.right)

        //更新RefreshView加载动画
        mCurrentDragPercent = targetPercent
        if (mListener != null && !isAction) {
            val pos = mFrom - mFrom * interpolatedTime
            mListener!!.onDragDistanceChange(pos,
                    mCurrentDragPercent, (pos - (mTarget!!.measuredWidth - mTarget!!.right)) / totalDragDistance)
        }

        //更新mTarget和mRefreshView的位置
        //        mTarget.setPadding(mTargetPaddingLeft, mTargetPaddingTop, mTargetPaddingRight, mTargetPaddingBottom + targetTop);
        setTargetOffsetRight(offset, false)

    }


    /**
     * 响应事件
     */
    private fun onAction() {
        ensureTarget()
        isAction = true

        if (!isAnimFinishDoAction) {
            if (mListener != null)
                mListener!!.onFinish()
        }


        //让mRefreshView停留几秒钟
        mRefreshView!!.post { animateOffsetToStartPosition() }
    }

    private val mToStartListener = object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation) {}

        override fun onAnimationRepeat(animation: Animation) {}

        override fun onAnimationEnd(animation: Animation) {
            mCurrentOffsetRight = mTarget!!.measuredWidth - mTarget!!.right//更新mCurrentOffsetTop

            if (isAction) {
                isAction = false
                if (isAnimFinishDoAction) {
                    if (mListener != null)
                        mListener!!.onFinish()
                }
            }
        }
    }


    /**
     * 处理多指触控的点击事件
     */
    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = MotionEventCompat.getActionIndex(ev)
        val pointerId = MotionEventCompat.getPointerId(ev, pointerIndex)
        if (pointerId == mActivePointerId) {
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex)
        }
    }

    private fun getMotionEventX(ev: MotionEvent, activePointerId: Int): Float {
        val index = MotionEventCompat.findPointerIndex(ev, activePointerId)
        if (index < 0) {
            return -1f
        }
        return MotionEventCompat.getX(ev, index)
    }
    private fun getMotionEventY(ev: MotionEvent, activePointerId: Int): Float {
        val index = MotionEventCompat.findPointerIndex(ev, activePointerId)
        if (index < 0) {
            return -1f
        }
        return MotionEventCompat.getY(ev, index)
    }

    /**
     * 通过调用setTargetOffsetRight方法
     * 更新mTarget和mBaseRefreshView的位置
     * 更新target滑动距离--mCurrentOffsetRight
     * @param offset         偏移位移
     * *
     * @param requiresUpdate 时候invalidate()
     */
    private fun setTargetOffsetRight(offset: Int, requiresUpdate: Boolean) {
        //        mRefreshView.bringToFront();
        mTarget!!.offsetLeftAndRight(-offset)
        mRefreshView!!.offsetLeftAndRight(-offset)
        mCurrentOffsetRight = mTarget!!.measuredWidth - mTarget!!.right
        if (requiresUpdate /*&& android.os.Build.VERSION.SDK_INT < 11*/) {
            invalidate()
        }
    }


    private fun canChildScrollRight(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget is AbsListView) {
                val absListView = mTarget as AbsListView?
                return absListView!!.childCount > 0 && (absListView.firstVisiblePosition > 0 || absListView.getChildAt(0)
                        .top < absListView.paddingTop)
            } else {
                return mTarget!!.scrollX > 0
            }
        } else {
            return ViewCompat.canScrollHorizontally(mTarget, 1)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

        ensureTarget()
        if (mTarget == null)
            return

        val height = measuredHeight
        val width = measuredWidth
        val left = paddingLeft
        val top = paddingTop
        val right = paddingRight
        val bottom = paddingBottom

        //mTarget MATCH_PARENT
        mTarget!!.layout(left - mCurrentOffsetRight, top, left + width - right - mCurrentOffsetRight, top + height)
        //mRefreshView隐藏在mTarget的上面
        mRefreshView!!.layout(mTarget!!.measuredWidth - mCurrentOffsetRight, top, mTarget!!.measuredWidth - mCurrentOffsetRight + mRefreshView.measuredWidth, top + height)
    }

    fun setOnDragListener(listener: OnDragListener) {
        mListener = listener
    }

    fun setmDragMode(dragMode: Int) {
        this.mDragMode = dragMode
    }

    /**
     * 滑动监听
     */
    interface OnDragListener {

        /**
         * 不要在此进行耗时的操作
         */
        fun onFinish()

        fun onDragDistanceChange(distance: Float, percent: Float, offset: Float)
    }

    companion object {

        private val UNLIMIT_DRAG_DISTANCE = 0//无限滑动距离模式
        private val LIMIT_DRAG_DISTANCE = 1//有限滑动距离模式
        private val DRAG_MAX_DISTANCE = 120//滑动的最大距离
        /**
         * 滑动拖拽阻尼
         */
        private val DRAG_RATE = .4f
        private val DECELERATE_INTERPOLATION_FACTOR = 2f

        val STYLE_SUN = 0
        val MAX_OFFSET_ANIMATION_DURATION = 700

        private val INVALID_POINTER = -1
    }

    /**
     * 设置查看更多是否可用
     */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if(enabled){
            mRefreshView!!.visibility=View.VISIBLE
        }else{
            mRefreshView!!.visibility=View.GONE
        }
        invalidate()
    }

}

