package cn.labzen.web.file.internal.pdf;

import cn.labzen.web.file.definition.bean.Schema;
import cn.labzen.web.file.internal.AbstractDataFileHandler;
import cn.labzen.web.file.internal.WritableDataBeanParser;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class PdfFileHandler<B> extends AbstractDataFileHandler<B> {

  public PdfFileHandler(@NonNull Class<B> type) {
    super(type);
  }

  @Override
  public void handle() {
    String regularFontPath = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("fonts/AlibabaPuHuiTi-3-55-Regular.ttf")).getPath();
    String boldFontPath = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("fonts/AlibabaPuHuiTi-3-85-Bold.ttf")).getPath();

    PdfWriter writer;
    PdfDocument pdfDoc;
    Document document;
    PdfFont regularFont;
    PdfFont boldFont;
    try {
      writer = new PdfWriter(outputFile);
      pdfDoc = new PdfDocument(writer);
      pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
      document = new Document(pdfDoc);
      document.setMargins(10, 10, 10, 10);
      regularFont = PdfFontFactory.createFont(regularFontPath, PdfEncodings.IDENTITY_H);
      boldFont = PdfFontFactory.createFont(boldFontPath, PdfEncodings.IDENTITY_H);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    WritableDataBeanParser parser = new WritableDataBeanParser(super.type);
    Schema schema = parser.parse();

    List<List<String>> heads = readSchemaHead(schema);

    Table table = new Table(heads.size());
    // 表格宽度占页面100%
    table.setWidth(100);

    // 填充表头
    for (List<String> name : heads) {
      Cell headerCell = new Cell()
        .add(new Paragraph("标题").setFont(boldFont).setFontSize(11))
        .setTextAlignment(TextAlignment.CENTER);
      table.addHeaderCell(headerCell);
    }

    List<Field> schemaIncludedFields = getSchemaIncludedFields(schema);
    // 遍历数据，填充表格行
    for (B datum : data) {
      for (Field field : schemaIncludedFields) {
        String text = safeGet(field, datum);

        Cell cell = new Cell()
          .add(new Paragraph(text).setFont(regularFont).setFontSize(10))
          .setTextAlignment(TextAlignment.CENTER);
        table.addCell(cell);
      }
    }

    document.add(table);
    pdfDoc.close();
    try {
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String safeGet(Field field, Object instance) {
    try {
      return field.get(instance).toString();
    } catch (IllegalAccessException e) {
      return "";
    }
  }

  private List<Field> getSchemaIncludedFields(Schema schema) {
    List<Field> allFields = Arrays.asList(type.getDeclaredFields());

    List<String> fieldNames = getSchemaIncludedFieldNames(schema);
    return fieldNames.stream()
      .map(fn ->
        allFields.stream().filter(f -> f.getName().equals(fn)).findFirst().orElseThrow())
      .peek(field -> field.setAccessible(true))
      .toList();
  }
}
