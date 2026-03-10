package cn.labzen.web.file.definition.bean;

import cn.labzen.tool.structure.Pair;
import cn.labzen.web.file.definition.enums.Alignment;
import cn.labzen.web.file.definition.enums.BorderWidth;
import lombok.Data;


@Data
public class Style implements Cloneable {

  public static final Pair<Alignment, Alignment> DEFAULT_ALIGNS = new Pair<>(Alignment.CENTER, Alignment.CENTER);
  public static final String DEFAULT_BACKGROUND_COLOR = "#FFFFFF";
  public static final int DEFAULT_FONT_SIZE = 11;
  public static final String DEFAULT_FONT_COLOR = "#000000";
  public static final boolean DEFAULT_FONT_BOLD = false;
  public static final BorderWidth DEFAULT_BORDER_WIDTH = BorderWidth.THIN;
  public static final boolean DEFAULT_WRAPPED = true;
  public static final boolean DEFAULT_HIDDEN = false;

  private Pair<Alignment, Alignment> align = DEFAULT_ALIGNS;
  private String backgroundColor = DEFAULT_BACKGROUND_COLOR;
  private short fontSize = DEFAULT_FONT_SIZE;
  private String fontColor = DEFAULT_FONT_COLOR;
  private boolean fontBold = DEFAULT_FONT_BOLD;
  private boolean wrapped = DEFAULT_WRAPPED;
  private BorderWidth borderWidth = DEFAULT_BORDER_WIDTH;
  private boolean hidden = DEFAULT_HIDDEN;

  public Style merge(Style style) {
    Style mergedStyle = this.clone();

    if (!DEFAULT_ALIGNS.equals(style.align)) {
      mergedStyle.setAlign(style.getAlign());
    }
    if (!DEFAULT_BACKGROUND_COLOR.equals(style.getBackgroundColor())) {
      mergedStyle.setBackgroundColor(style.getBackgroundColor());
    }
    if (DEFAULT_FONT_SIZE != style.getFontSize()) {
      mergedStyle.setFontSize(style.getFontSize());
    }
    if (!DEFAULT_FONT_COLOR.equals(style.getFontColor())) {
      mergedStyle.setFontColor(style.getFontColor());
    }
    if (DEFAULT_FONT_BOLD != style.isFontBold()) {
      mergedStyle.setFontBold(style.isFontBold());
    }
    if (DEFAULT_BORDER_WIDTH != style.getBorderWidth()) {
      mergedStyle.setBorderWidth(style.getBorderWidth());
    }
    if (DEFAULT_WRAPPED != style.isWrapped()) {
      mergedStyle.setWrapped(style.isWrapped());
    }
    if (DEFAULT_HIDDEN != style.isHidden()) {
      mergedStyle.setHidden(style.isHidden());
    }

    return mergedStyle;
  }

  @Override
  public Style clone() {
    try {
      return (Style) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
