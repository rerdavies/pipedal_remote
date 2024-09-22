package com.twoplay.pipedal;

/**
 * Copyright (c) 2024, sRobin Davies
 * Created by Robin on 16/09/2024.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.util.TypedValue;

import org.xml.sax.XMLReader;

public class IndentTextHandler {
    public static Spanned Translate(Context context, String htmlString) {

        return Html.fromHtml(htmlString, 0, null, new CustomTagHandler(context));
    }

    private static class CustomTagHandler implements Html.TagHandler {
        private static final int BULLET_MARGIN_DP = 8;
        private static final int LIST_ITEM_INDENT_DP = 8;
        private static final int BULLET_SIZE_DP = 6;
        private final int bulletSizePx;
        private final int bulletMarginPx;
        private final int listItemIndentPx;

        CustomTagHandler(Context context) {
            this.bulletMarginPx = dpToPx(context, BULLET_MARGIN_DP);
            this.bulletSizePx = dpToPx(context,BULLET_SIZE_DP);
            this.listItemIndentPx = dpToPx(context, LIST_ITEM_INDENT_DP);
        }

        private int dpToPx(Context context, int dp) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dp,
                    context.getResources().getDisplayMetrics()
            );
        }

        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
            if (tag.equalsIgnoreCase("xul")) {
                if (opening) {
                    output.setSpan(new ULLeadingMarginSpan(listItemIndentPx), output.length(), output.length(), Spanned.SPAN_MARK_MARK);
                } else {
                    Object span = getLast(output, ULLeadingMarginSpan.class);
                    int where = output.getSpanStart(span);
                    output.removeSpan(span);
                    if (where != output.length()) {
                        output.setSpan(new ULLeadingMarginSpan(listItemIndentPx), where, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            } else if (tag.equalsIgnoreCase("xli")) {
                if (opening) {
                    output.setSpan(new BulletSpan(bulletMarginPx,0,bulletSizePx), output.length(), output.length(), Spanned.SPAN_MARK_MARK);
                } else {
                    Object span = getLast(output, BulletSpan.class);
                    int where = output.getSpanStart(span);
                    output.removeSpan(span);
                    if (where != output.length()) {
                        output.setSpan(new BulletSpan(bulletMarginPx), where, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }

        private Object getLast(Editable text, Class kind) {
            Object[] objs = text.getSpans(0, text.length(), kind);
            if (objs.length == 0) {
                return null;
            } else {
                return objs[objs.length - 1];
            }
        }
    }

    private static class ULLeadingMarginSpan implements LeadingMarginSpan {
        private final int margin;

        ULLeadingMarginSpan(int margin) {
            this.margin = margin;
        }

        @Override
        public int getLeadingMargin(boolean first) {
            return margin;
        }
        @Override
        public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout layout) {

        }
    }
}