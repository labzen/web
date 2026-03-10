package cn.labzen.web.file.internal.excel;

import cn.labzen.web.file.definition.bean.Column;
import cn.labzen.web.file.definition.bean.Schema;
import cn.labzen.web.file.definition.bean.Style;
import cn.labzen.web.file.definition.enums.Alignment;
import cn.labzen.web.file.definition.enums.BorderWidth;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.google.common.collect.Maps;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;

import java.util.List;
import java.util.Map;

/**
 * 单元格样式处理
 */
public class CellStyleHandler extends AbstractCellWriteHandler {

  private static final short DEFAULT_BORDER_COLOR = IndexedColors.BLACK.getIndex();

  private final Map<String, HSSFColor> definedHssfColors = Maps.newHashMap();
  private final Map<String, XSSFColor> definedXssfColors = Maps.newHashMap();


  public CellStyleHandler(Schema schema) {
    super(schema);
  }

  /**
   * 在单元格处置的最后步骤，对单元格设置样式
   */
  @Override
  public void afterCellDispose(WriteSheetHolder writeSheetHolder,
                               WriteTableHolder writeTableHolder,
                               List<WriteCellData<?>> cellDataList,
                               Cell cell,
                               Head head,
                               Integer relativeRowIndex,
                               Boolean isHead) {
    Column currentColumn = super.findColumn(cell);
    if (currentColumn == null) {
      return;
    }

    Style currentStyle = isHead ? currentColumn.getStyleHeader() : currentColumn.getStyleContent();
    CellStyle originCellStyle = cell.getCellStyle();
    Workbook workbook = writeSheetHolder.getSheet().getWorkbook();

    CellStyle cellStyle = workbook.createCellStyle();
    cellStyle.cloneStyleFrom(originCellStyle);

    buildCellStyle(workbook, cellStyle, currentStyle);
    cell.setCellStyle(cellStyle);
  }

  private void buildCellStyle(Workbook workbook, CellStyle cellStyle, Style style) {
    if (style.isHidden()) {
      cellStyle.setHidden(true);
    }
    if (style.isWrapped()) {
      cellStyle.setWrapText(true);
    }
    cellStyle.setAlignment(convertHorizontalAlignment(style.getAlign().first()));
    cellStyle.setVerticalAlignment(convertVerticalAlignment(style.getAlign().second()));

    if (workbook instanceof HSSFWorkbook hwb) {
      cellStyle.setFillForegroundColor(convertHssfColor(style.getBackgroundColor(), hwb));
    } else {
      cellStyle.setFillForegroundColor(convertXssfColor(style.getBackgroundColor()));
    }
    cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

    BorderStyle borderStyle = convertBorderStyle(style.getBorderWidth());
    cellStyle.setBorderTop(borderStyle);
    cellStyle.setBorderRight(borderStyle);
    cellStyle.setBorderBottom(borderStyle);
    cellStyle.setBorderLeft(borderStyle);
    cellStyle.setTopBorderColor(DEFAULT_BORDER_COLOR);
    cellStyle.setRightBorderColor(DEFAULT_BORDER_COLOR);
    cellStyle.setBottomBorderColor(DEFAULT_BORDER_COLOR);
    cellStyle.setLeftBorderColor(DEFAULT_BORDER_COLOR);

    // ---- 字体
    Font originFont = null;
    if (cellStyle instanceof HSSFCellStyle hcs) {
      originFont = hcs.getFont(workbook);
    }
    if (cellStyle instanceof XSSFCellStyle xcs) {
      originFont = xcs.getFont();
    }
    Font font = createAndCopyFont(workbook, originFont);

    font.setFontHeightInPoints(style.getFontSize());
    if (style.isFontBold()) {
      font.setBold(true);
    }
    if (font instanceof HSSFFont hf) {
      assert workbook instanceof HSSFWorkbook;
      HSSFColor hssfColor = convertHssfColor(style.getFontColor(), (HSSFWorkbook) workbook);
      hf.setColor(hssfColor.getIndex());
    } else if (font instanceof XSSFFont xf) {
      xf.setColor(convertXssfColor(style.getFontColor()));
    }

    cellStyle.setFont(font);
  }

  @SuppressWarnings("DuplicatedCode")
  private Font createAndCopyFont(Workbook workbook, Font originFont) {
    Font font = workbook.createFont();
    switch (originFont) {
      case null -> {
        return font;
      }
      case XSSFFont oxf -> {
        XSSFFont xf = (XSSFFont) font;
        xf.setFontName(oxf.getFontName());
        xf.setFontHeightInPoints(oxf.getFontHeightInPoints());
        xf.setItalic(oxf.getItalic());
        xf.setStrikeout(oxf.getStrikeout());
        xf.setColor(oxf.getColor());
        xf.setTypeOffset(oxf.getTypeOffset());
        xf.setUnderline(oxf.getUnderline());
        xf.setCharSet(oxf.getCharSet());
        xf.setBold(oxf.getBold());
        return xf;
      }
      case HSSFFont ohf -> {
        HSSFFont hf = (HSSFFont) font;
        hf.setFontName(ohf.getFontName());
        hf.setFontHeightInPoints(ohf.getFontHeightInPoints());
        hf.setItalic(ohf.getItalic());
        hf.setStrikeout(ohf.getStrikeout());
        hf.setColor(ohf.getColor());
        hf.setTypeOffset(ohf.getTypeOffset());
        hf.setUnderline(ohf.getUnderline());
        hf.setCharSet(ohf.getCharSet());
        hf.setBold(ohf.getBold());
        return hf;
      }
      default -> {
      }
    }
    return font;
  }

  private HorizontalAlignment convertHorizontalAlignment(Alignment alignment) {
    return switch (alignment) {
      case CENTER -> HorizontalAlignment.CENTER;
      case LEFT -> HorizontalAlignment.LEFT;
      case RIGHT -> HorizontalAlignment.RIGHT;
      case JUSTIFY -> HorizontalAlignment.JUSTIFY;
      case DISTRIBUTED -> HorizontalAlignment.DISTRIBUTED;
      case FILL -> HorizontalAlignment.FILL;
      default -> HorizontalAlignment.GENERAL;
    };
  }

  private VerticalAlignment convertVerticalAlignment(Alignment alignment) {
    return switch (alignment) {
      case TOP -> VerticalAlignment.TOP;
      case BOTTOM -> VerticalAlignment.BOTTOM;
      case JUSTIFY -> VerticalAlignment.JUSTIFY;
      case DISTRIBUTED -> VerticalAlignment.DISTRIBUTED;
      default -> VerticalAlignment.CENTER;
    };
  }

  /**
   * 将十六进制颜色字符串转换为 XSSFColor
   *
   * @param hexColor 十六进制颜色字符串，如 "#000000" 或 "000000"
   * @return XSSFColor 对象
   */
  private XSSFColor convertXssfColor(String hexColor) {
    return definedXssfColors.computeIfAbsent(hexColor, k -> {
      String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
      int r = Integer.parseInt(hex.substring(0, 2), 16);
      int g = Integer.parseInt(hex.substring(2, 4), 16);
      int b = Integer.parseInt(hex.substring(4, 6), 16);

      // 3. 创建 XSSFColor（使用 byte 数组）
      byte[] rgb = new byte[]{(byte) r, (byte) g, (byte) b};
      return new XSSFColor(rgb, null);
    });
  }

  /**
   * 将十六进制颜色字符串添加为自定义颜色
   *
   * @param hexColor 十六进制颜色字符串，如 "#000000" 或 "000000"
   * @param workbook 当前的 HSSFWorkbook 对象
   */
  public HSSFColor convertHssfColor(String hexColor, HSSFWorkbook workbook) {
    return definedHssfColors.computeIfAbsent(hexColor, k -> {
      String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
      byte r = (byte) Integer.parseInt(hex.substring(0, 2), 16);
      byte g = (byte) Integer.parseInt(hex.substring(2, 4), 16);
      byte b = (byte) Integer.parseInt(hex.substring(4, 6), 16);

      HSSFPalette palette = workbook.getCustomPalette();
      // addColor 会尝试添加到空闲位置
      return palette.addColor(r, g, b);
    });
  }

  private BorderStyle convertBorderStyle(BorderWidth borderWidth) {
    return switch (borderWidth) {
      case MEDIUM -> BorderStyle.MEDIUM;
      case THICK -> BorderStyle.THICK;
      case DOUBLE -> BorderStyle.DOUBLE;
      default -> BorderStyle.THIN;
    };
  }
}
