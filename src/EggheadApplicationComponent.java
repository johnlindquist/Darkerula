import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.ex.DefaultColorSchemesManager;
import com.intellij.openapi.editor.colors.impl.EditorColorsSchemeImpl;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.ui.JBColor;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by johnlindquist on 11/1/14.
 */
public class EggheadApplicationComponent implements ApplicationComponent {

    private final String eggheadDark = "EggheadDark";

    @Override
    public void initComponent() {
        try {
            loadScheme();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            UIManager.setLookAndFeel(new EggheadDarkLAF());
            JBColor.setDark(true);
            IconLoader.setUseDarkIcons(true);
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        final EditorColorsScheme scheme = EditorColorsManager.getInstance().getScheme("Egghead.io Dark");
        if (scheme != null) {
            EditorColorsManager.getInstance().setGlobalScheme(scheme);
        }


        UISettings.getInstance().fireUISettingsChanged();
        ActionToolbarImpl.updateAllToolbarsImmediately();
    }

    @Override
    public void disposeComponent() {

    }

    @NotNull
    @Override
    public String getComponentName() {
        return "EggheadApplicationComponent";
    }


    private void loadScheme() throws Exception {
        String fileName = eggheadDark + ".xml";


        InputStream stream = getClass().getResourceAsStream(fileName);
        try {
            EditorColorsSchemeImpl scheme = loadSchemeFromStream(fileName, stream);
            if (scheme != null) {
                EditorColorsManager.getInstance().addColorsScheme(scheme);
            }
        } catch (final Exception e) {
            throw e;
        }
    }

    private static EditorColorsSchemeImpl loadSchemeFromStream(String schemePath, InputStream inputStream)
            throws IOException, JDOMException, InvalidDataException {
        if (inputStream == null) {
            // Error shouldn't occur during this operation
            // thus we report error instead of info
            return null;
        }

        final Document document;
        try {
            document = JDOMUtil.loadDocument(inputStream);
        } catch (JDOMException e) {
            throw e;
        }
        return loadSchemeFromDocument(document, false);
    }

    @NotNull
    private static EditorColorsSchemeImpl loadSchemeFromDocument(final Document document,
                                                                 final boolean isEditable)
            throws InvalidDataException {

        final Element root = document.getRootElement();

        final EditorColorsSchemeImpl scheme = new EditorColorsSchemeImpl(null, DefaultColorSchemesManager.getInstance());
        scheme.readExternal(root);
        return scheme;
    }

}
