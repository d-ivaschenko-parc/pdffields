package one.doc;


import com.itextpdf.forms.fields.PdfButtonFormField;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;


public class Fields {


    private void getFields(String path) throws IOException {
        // Create a reader to extract info
        PdfReader reader = new PdfReader(path);
        reader.setUnethicalReading(true);
        //PdfDocument pdfDoc = new PdfDocument(reader, writer);
        PdfDocument pdfDoc = new PdfDocument(reader);
        // Get the fields from the reader (read-only!!!)
        PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);
        // Loop over the fields and get info about them
        Set<String> fields = form.getFormFields().keySet();
        String parent = "";
        for (String key : fields) {

            PdfFormField field = form.getField(key);
            PdfName type = field.getFormType();

            String keyTag;
            String lineOut = "";
            keyTag = "<key>" + key + "</key>";
            if(type != null) {

                if (0 == PdfName.Btn.compareTo(type)) {
                    try {
                        PdfButtonFormField pbff = (PdfButtonFormField)form.getField(key);
                        if (pbff.isPushButton()) {
                            continue;
                        } else {
                            if(pbff.isRadio()){
                                parent = lineOut = keyTag + "<type>Radiobutton</type>";
                            }else {
                                parent = lineOut = keyTag + "<type>Checkbox</type>";
                            }
                        }
                    } catch (Exception e) {
                        lineOut = keyTag + "<type>notype</type><parent>" + parent + "</parent>";
                    }

                } else if (0 == PdfName.Ch.compareTo(type)) {
                    parent = lineOut = keyTag + "<type>Choicebox</type>";
                } else if (0 == PdfName.Sig.compareTo(type)) {
                    parent = lineOut = keyTag + "<type>Signature</type>";
                } else if (0 == PdfName.Tx.compareTo(type)) {
                    parent = lineOut = keyTag + "<type>Text</type>";
                }else {
                    parent = lineOut = "?";
                }

            } else {
                lineOut = keyTag + "<type>notype</type>";
            }

            String[] ss = field.getAppearanceStates();

            System.out.println("<field>" + lineOut +  "<opts>" + implode(",", ss) + "</opts><value>" + field.getValueAsString() + "</value></field>");
        }
        pdfDoc.close();
    }

    private static String implode(String glue, String[] strArray)
    {
        String ret = "";
        for(int i=0;i<strArray.length;i++)
        {
            ret += (i == strArray.length - 1) ? strArray[i] : strArray[i] + glue;
        }
        return ret;
    }

    private void highlightField(String path, String out) throws IOException
    {
        PdfWriter writer = new PdfWriter(out);
        PdfReader reader = new PdfReader(path);
        reader.setUnethicalReading(true);
        PdfDocument pdfDoc = new PdfDocument(reader, writer);

        PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);
        this.highlightForm(form);

        pdfDoc.close();
    }


    private void highlightForm(PdfAcroForm form)
    {
        Set<String> fields = form.getFormFields().keySet();
        for (String key : fields) {

            PdfFormField field = form.getField(key);
            try{
                field.setBackgroundColor(ColorConstants.LIGHT_GRAY);
                PdfName type = field.getFormType();
                if (0 != PdfName.Tx.compareTo(type)) {
                    field.setValue("Off");
                }
            } catch (Exception e) {

            }
        }
    }

    private void setValues2(String path, String out, String jsonPath, String highlight) throws IOException
    {
        JSONParser parser = new JSONParser();
        PdfWriter writer = new PdfWriter(out);
        PdfReader reader = new PdfReader(path);
        reader.setUnethicalReading(true);
        PdfDocument pdfDoc = new PdfDocument(reader, writer);
        PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);

        if (highlight.equals("1")) {
            this.highlightForm(form);
        }

        try {
            Object obj = parser.parse(new FileReader(jsonPath));
            JSONArray jsonArray = (JSONArray) obj;

            for (Object jsonObject : jsonArray) {
                String key = ((JSONObject) jsonObject).get("key").toString();
                String value = "";
                try{
                    value = ((JSONObject) jsonObject).get("value").toString();
                } catch (Exception e) {
                    value = "";
                }


                PdfFormField field = form.getField(key);
                if (field == null) {
                    continue;
                }
                PdfName type = field.getFormType();
                if (0 != PdfName.Tx.compareTo(type)) {
                    field.setCheckType(PdfFormField.TYPE_CHECK);
                }
                field.setValue(value);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        pdfDoc.close();
    }


    /**
     * This will read a PDF file and print out the form elements. <br>
     * see usage() for commandline
     *
     * @param args command line arguments
     *
     * @throws IOException If there is an error importing the FDF document.
     */
    public static void main(String[] args) throws IOException
    {
        Fields fields = new Fields();
        if (args[0].equals("get_fields")) {
            fields.getFields(args[1]);
        }else if (args[0].equals("highlight_fields")) {
            fields.highlightField(args[1], args[2]);
        }else if (args[0].equals("set_values")) {
            fields.setValues2(args[1], args[2], args[3], args[4]);
        }

    }

}
