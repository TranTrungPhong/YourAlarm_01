package com.framgia.youralarm1.adapter;
import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import com.framgia.youralarm1.R;
import com.framgia.youralarm1.activity.MainActivity;
import com.framgia.youralarm1.contstant.Const;
import com.framgia.youralarm1.models.ItemAlarm;
import com.framgia.youralarm1.util.PreferenceUtil;
import com.framgia.youralarm1.utils.AlarmUtils;
import com.framgia.youralarm1.utils.ParseTimeUtils;
import com.framgia.youralarm1.widget.CircularButton;
import java.util.Arrays;
import java.util.List;

/**
 * Created by vuduychuong1994 on 6/3/16.
 */
public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.ViewHolder> implements
        CircularButton.OnClickInteractionListener, View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {

    private Context mContext;
    private List<ItemAlarm> mAlarmList;
    private int mLayoutExpandHeight;
    private OnAdapterInterActionListener mOnAdapterInterActionListener;
    private boolean isClickExpand = false;
    private boolean isCheckFloat = false;
    private FloatingActionButton mFab;
    public AlarmAdapter(Context context, List<ItemAlarm> alarmList, FloatingActionButton button) {
        mContext = context;
        mAlarmList = alarmList;
        mFab = button;
        if (mContext instanceof OnAdapterInterActionListener)
            mOnAdapterInterActionListener = (OnAdapterInterActionListener) mContext;
        else {
            throw new RuntimeException(
                    context.toString() + context.getString(R.string.error_implement_alarm_adapter));
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mView.setTag(position);
        holder.mLayoutParent.setTag(position);
        holder.mItemAlarm = mAlarmList.get(position);
        CircularButton[] arrCircularButton
                = new CircularButton[]{holder.mCircularButtonSunday,
                holder.mCircularButtonMonday, holder.mCircularButtonTuesday,
                holder.mCircularButtonWednesday, holder.mCircularButtonThursday,
                holder.mCircularButtonFriday, holder.mCircularButtonSaturday};
        for (CircularButton circularButton : Arrays.asList(arrCircularButton)) {
            circularButton.setTag(position);
        }
        holder.mTextTime.setTag(position);
        holder.mTextSelectRingtone.setTag(position);
        holder.mImageActionDelete.setTag(position);
        holder.mImageExpandItem.setTag(position);
        holder.mSwitchCompat.setTag(position);
        holder.mCheckBoxVibrate.setTag(position);
        holder.mTextTitle.setTag(position);

        ItemAlarm itemAlarm = mAlarmList.get(position);
        holder.mTextTime.setText(ParseTimeUtils.formatTextTime(itemAlarm.getTime()));
        holder.mSwitchCompat.setChecked(itemAlarm.isStatus());

        if (itemAlarm.isExpand()) {
            setExpand(holder, isClickExpand);
            if (isClickExpand) isClickExpand = false;
            ItemAlarm.WeekDay[] arrWeekday = new ItemAlarm.WeekDay[]{ItemAlarm.WeekDay.SUNDAY,
                    ItemAlarm.WeekDay.MONDAY, ItemAlarm.WeekDay.TUESDAY,
                    ItemAlarm.WeekDay.WEDNESDAY, ItemAlarm.WeekDay.THURSDAY,
                    ItemAlarm.WeekDay.FRIDAY, ItemAlarm.WeekDay.SATURDAY};
            setSelectedButtonWeekday(arrCircularButton, arrWeekday, itemAlarm);

            String ringToneName;
            if (itemAlarm.getRingTonePath() != null && !itemAlarm.getRingTonePath().equals(""))
                ringToneName = RingtoneManager.getRingtone(mContext,
                                                           Uri.parse(itemAlarm.getRingTonePath()))
                                                                .getTitle(mContext);
            else {
                PreferenceUtil preferenceUtil = new PreferenceUtil(mContext);
                Uri currenturi;
                try {
                    currenturi = Uri.parse(preferenceUtil.getStringData(Const.MY_PREFERENCES,
                                                                        Const.PRE_ALARM_SOUND));
                    if (currenturi.equals(Settings.System.DEFAULT_ALARM_ALERT_URI))
                        currenturi = RingtoneManager.getActualDefaultRingtoneUri(mContext,
                                                                                 RingtoneManager.TYPE_RINGTONE);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                        currenturi = RingtoneManager.getActualDefaultRingtoneUri(mContext,
                                                                                 RingtoneManager.TYPE_RINGTONE);
                }
                ringToneName = RingtoneManager.getRingtone(mContext, currenturi).getTitle(mContext);
                ringToneName += Const.DEFAULT;
            }
            holder.mTextSelectRingtone.setText(ringToneName);
            holder.mCheckBoxVibrate.setChecked(itemAlarm.isVibrate());
            holder.mTextTitle.setText(itemAlarm.getTitle());
        } else {
            if (holder.mLayoutExpand.getVisibility() == View.VISIBLE)
                setCollapse(holder, true);
            if (itemAlarm.getTitle() != null)
                holder.mTextTitleCollapse.setText(itemAlarm.getTitle());
        }
        setEvent(holder, position);
    }

    private void setSelectedButtonWeekday(CircularButton[] arrCircularButton,
                                          ItemAlarm.WeekDay[] arrWeekday, ItemAlarm itemAlarm) {
        for (int i = 0; i < arrCircularButton.length; i++) {
            arrCircularButton[i].setSelected(
                    itemAlarm.getWeekDayHashMap().containsKey(arrWeekday[i]) ?
                            itemAlarm.getWeekDayHashMap().get(arrWeekday[i]) : false);
        }
    }

    private void setEvent(final ViewHolder holder, final int position) {
        holder.mSwitchCompat.setOnCheckedChangeListener(this);
        holder.mTextTime.setOnClickListener(this);
        if ( holder.mItemAlarm.isExpand() ) {
            holder.mCheckBoxVibrate.setOnCheckedChangeListener(this);
            holder.mTextSelectRingtone.setOnClickListener(this);
            holder.mCircularButtonSunday.setOnClickInteractionListener(this);
            holder.mCircularButtonMonday.setOnClickInteractionListener(this);
            holder.mCircularButtonTuesday.setOnClickInteractionListener(this);
            holder.mCircularButtonWednesday.setOnClickInteractionListener(this);
            holder.mCircularButtonThursday.setOnClickInteractionListener(this);
            holder.mCircularButtonFriday.setOnClickInteractionListener(this);
            holder.mCircularButtonSaturday.setOnClickInteractionListener(this);
            holder.mImageActionDelete.setOnClickListener(this);
            holder.mTextTitle.setOnClickListener(this);
        }
        holder.mImageExpandItem.setOnClickListener(this);
        holder.mLayoutParent.setOnClickListener(this);
    }

    private void selectedView(int position) {
        if (mAlarmList.get(position).isExpand()) {
            mAlarmList.get(position).setExpand(false);
            checkFloatAddAlarm(false);
        } else {
            for (ItemAlarm itemAlarm : mAlarmList) {
                itemAlarm.setExpand(false);
            }
            mAlarmList.get(position).setExpand(true);
            checkFloatAddAlarm(true);
        }
        notifyDataSetChanged();
    }

    private void setExpand(ViewHolder holder, boolean haveAnimation) {
        holder.mTextTitleCollapse.setVisibility(View.GONE);
        holder.mImageActionDelete.setVisibility(View.VISIBLE);
        holder.mView.setBackgroundResource(R.color.bg_item_alarm_selected);
        expand(holder.mLayoutExpand, haveAnimation);
    }

    private void setCollapse(ViewHolder holder, boolean haveAnimation) {
        holder.mImageActionDelete.setVisibility(View.GONE);
        holder.mTextTitleCollapse.setVisibility(View.VISIBLE);
        holder.mView.setBackgroundResource(R.color.bg_item_alarm);
        collapse(holder.mLayoutExpand, haveAnimation);
    }

    private void expand(View summary, boolean isAnimation) {
        summary.setVisibility(View.VISIBLE);
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
//        summary.measure(widthSpec, Const.HEIGHT_MEASURE_SPEC_DEFAULT);
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        int density = metrics.densityDpi;
        ValueAnimator mAnimator
                = slideAnimator(0,
                Const.HEIGHT_MEASURE_SPEC_DEFAULT * density / DisplayMetrics.DENSITY_DEFAULT,
                summary);
        if (! isAnimation) mAnimator.setDuration(0);
        mAnimator.start();
    }

    private void collapse(final View summary, boolean isAnimation) {
        mLayoutExpandHeight = summary.getHeight();
        ValueAnimator mAnimator = slideAnimator(mLayoutExpandHeight, 0, summary);
        mAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animator) {
                summary.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        if (! isAnimation) mAnimator.setDuration(0);
        mAnimator.start();
    }

    private ValueAnimator slideAnimator(int start, int end, final View summary) {

        ValueAnimator animator = ValueAnimator.ofInt(start, end);


        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                //Update Height
                int value = (Integer) valueAnimator.getAnimatedValue();

                ViewGroup.LayoutParams layoutParams = summary.getLayoutParams();
                layoutParams.height = value;
                summary.setLayoutParams(layoutParams);
            }
        });
        return animator;
    }


    @Override
    public int getItemCount() {
        if (mAlarmList != null) return mAlarmList.size();
        else return 0;
    }

    @Override
    public void onClickInteractionListener(final View view, int id, boolean isSelected) {
        int position = (int) view.getTag();
        mOnAdapterInterActionListener.onChooseWeekdayListener(id, position, isSelected);
    }

    @Override
    public void onClick(final View v) {
        final int position = (int) v.getTag();
        ItemAlarm itemAlarm = mAlarmList.get(position);
        switch (v.getId()){
            case R.id.text_time:
                TimePickerDialog timePickerDialog =
                        new TimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                if (view.isShown()) {
                                    ((TextView) v).setText(ParseTimeUtils.
                                            formatTextTime(hourOfDay * 60 + minute));
                                    mAlarmList.get(position).setTime(hourOfDay * 60 + minute);
                                    mOnAdapterInterActionListener.onChangedAlarm(position);
                                }
                            }
                        }, itemAlarm.getTime() / 60, itemAlarm.getTime() % 60, true);
                timePickerDialog.show();
                break;
            case R.id.text_select_ringtone:
                String ringToneUri;
                if (itemAlarm.getRingTonePath() == null) {
                    Uri currenturi =
                            RingtoneManager.getActualDefaultRingtoneUri(mContext,
                                                                        RingtoneManager.TYPE_ALARM);
                    ringToneUri = String.valueOf(RingtoneManager.getRingtone(mContext, currenturi));
                } else ringToneUri = mAlarmList.get(position).getRingTonePath();

                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                                mContext.getString(R.string.choose_ringtone_title));
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                                RingtoneManager.TYPE_ALARM);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringToneUri);
                ((Activity) mContext).startActivityForResult(intent,
                                                             Const.CHOOSE_RINGTONE_REQUEST);
                break;
            case R.id.text_title:
                openDialogToEditTitle(v, position);
                break;
            case R.id.image_expand_item:
                isClickExpand = true;
                selectedView(position);
                break;
            case R.id.image_action_delete:
                mOnAdapterInterActionListener.onDeletedAlarm(position);
//                    checkFloatAddAlarm(false);
                break;
            case R.id.layout_parent:
                isClickExpand = true;
                selectedView(position);
                break;
            default:
                break;
        }
    }

    public void checkFloatAddAlarm(boolean isCheck){
        if(isCheck){
            mFab.setVisibility(View.GONE);
        }else{
            mFab.setVisibility(View.VISIBLE);
        }
    }

    private void openDialogToEditTitle(final View v, final int position) {
        final ItemAlarm itemAlarm = mAlarmList.get(position);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        alertDialog.setTitle(R.string.title);
        alertDialog.setMessage(R.string.enter_title);

        final EditText input = new EditText(mContext);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        input.setText(itemAlarm.getTitle());
        input.setSelection(input.getText().length());
        alertDialog.setView(input);
        alertDialog.setIcon(R.drawable.alarm);
        //TODO: Set action for alert dialog
        alertDialog.setPositiveButton(mContext.getString(R.string.ok),
                                      new DialogInterface.OnClickListener() {
                                          public void onClick(DialogInterface dialog, int which) {
                                              String tempTitle = input.getText().toString();
                                              if (!tempTitle.equals(itemAlarm.getTitle())) {
                                                  itemAlarm.setTitle(tempTitle);
                                                  ((TextView) v).setText(tempTitle);
                                                  mOnAdapterInterActionListener.onChangedAlarm(position);

                                              }
                                          }
                                      });

        alertDialog.setNegativeButton(mContext.getString(R.string.cancel),
                                      new DialogInterface.OnClickListener() {
                                          public void onClick(DialogInterface dialog, int which) {
                                              dialog.cancel();
                                          }
                                      });

        alertDialog.show();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int position = (int) buttonView.getTag();
        ItemAlarm itemAlarm = mAlarmList.get(position);
        switch (buttonView.getId()) {
            case R.id.checkbox_vibrate:
                itemAlarm.setVibrate(isChecked);
                mOnAdapterInterActionListener.onChangedAlarm(position);
                break;
            case R.id.switch_status:
                itemAlarm.setStatus(isChecked);
                if (itemAlarm.isStatus())
                    mOnAdapterInterActionListener.onChangedAlarm(position);
                else {
                    AlarmUtils.cancelAlarm(mContext, itemAlarm);
                    Toast.makeText(mContext, R.string.alarm_cancelled, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public View mView;
        public LinearLayout mLayoutParent;
        public AppCompatTextView mTextTime;
        public SwitchCompat mSwitchCompat;
        public CircularButton mCircularButtonSunday;
        public CircularButton mCircularButtonMonday;
        public CircularButton mCircularButtonTuesday;
        public CircularButton mCircularButtonWednesday;
        public CircularButton mCircularButtonThursday;
        public CircularButton mCircularButtonFriday;
        public CircularButton mCircularButtonSaturday;
        public AppCompatTextView mTextSelectRingtone;
        public AppCompatCheckBox mCheckBoxVibrate;
        public TextView mTextTitle;
        public AppCompatTextView mTextTitleCollapse;
        public LinearLayout mLayoutExpand;
        public ImageView mImageActionDelete;
        public ImageView mImageExpandItem;
        public ItemAlarm mItemAlarm;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            setView(mView);

        }
        private void setView(View view) {
            mLayoutParent = (LinearLayout) view.findViewById(R.id.layout_parent);
            mTextTime = (AppCompatTextView) view.findViewById(R.id.text_time);
            mSwitchCompat = (SwitchCompat) view.findViewById(R.id.switch_status);
            mCircularButtonSunday = (CircularButton) view.findViewById(R.id.circle_button_sunday);
            mCircularButtonMonday = (CircularButton) view.findViewById(R.id.circle_button_monday);
            mCircularButtonTuesday = (CircularButton) view.findViewById(R.id.circle_button_tuesday);
            mCircularButtonWednesday =
                    (CircularButton) view.findViewById(R.id.circle_button_wednesday);
            mCircularButtonThursday =
                    (CircularButton) view.findViewById(R.id.circle_button_thursday);
            mCircularButtonFriday = (CircularButton) view.findViewById(R.id.circle_button_friday);
            mCircularButtonSaturday =
                    (CircularButton) view.findViewById(R.id.circle_button_saturday);
            mTextSelectRingtone = (AppCompatTextView) view.findViewById(R.id.text_select_ringtone);
            mCheckBoxVibrate = (AppCompatCheckBox) view.findViewById(R.id.checkbox_vibrate);
            mTextTitle = (TextView) view.findViewById(R.id.text_title);
            mTextTitleCollapse = (AppCompatTextView) view.findViewById(R.id.text_title_collapse);
            mLayoutExpand = (LinearLayout) view.findViewById(R.id.layout_expand);
            mImageActionDelete = (ImageView) view.findViewById(R.id.image_action_delete);
            mImageExpandItem = (ImageView) view.findViewById(R.id.image_expand_item);
        }
    }

    public interface OnAdapterInterActionListener {
        void onChooseWeekdayListener(int idView, int position, boolean iSelected);

        void onDeletedAlarm(int position);

        void onChangedAlarm(int position);
    }
}