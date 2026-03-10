package cn.labzen.web.file.internal.excel;

import cn.labzen.web.file.definition.bean.Column;
import cn.labzen.web.file.definition.bean.Schema;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.google.common.collect.Maps;
import org.apache.poi.ss.usermodel.Cell;

import java.util.List;
import java.util.Map;

/**
 * 列宽处理器
 */
public class ColumnWidthHandler extends AbstractCellWriteHandler {

  private static final int DEFAULT_WIDTH = 10;

  private final Map<Integer, Integer> columnWidthMap = Maps.newHashMap();

  public ColumnWidthHandler(Schema schema) {
    super(schema);

    for (Column column : schema.getColumns()) {
      int width = column.getWidth();
      if (width <= 0) {
        width = DEFAULT_WIDTH;
      }
      columnWidthMap.put(column.getIndex(), width);
    }
  }

  @Override
  public void afterCellDispose(WriteSheetHolder writeSheetHolder,
                               WriteTableHolder writeTableHolder,
                               List<WriteCellData<?>> cellDataList,
                               Cell cell,
                               Head head,
                               Integer relativeRowIndex,
                               Boolean isHead) {
    if (!isHead) {
      return;
    }

    int columnIndex = cell.getColumnIndex();
    Integer width = columnWidthMap.getOrDefault(columnIndex, DEFAULT_WIDTH);
    writeSheetHolder.getSheet().setColumnWidth(columnIndex, width * 256);
  }
}
