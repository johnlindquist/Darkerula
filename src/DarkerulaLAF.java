import com.intellij.ide.ui.laf.IdeaLaf;
import com.intellij.ide.ui.laf.LafManagerImpl;
import com.intellij.ide.ui.laf.darcula.DarculaLaf;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.containers.hash.HashMap;
import com.intellij.util.ui.UIUtil;
import sun.awt.AppContext;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicLookAndFeel;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Properties;

/**
 * Created by johnlindquist on 11/1/14.
 * Overriding/copying @kb's work
 */
public class DarkerulaLAF extends DarculaLaf {

    BasicLookAndFeel base;


    public DarkerulaLAF() {
        try {
            if (SystemInfo.isWindows || SystemInfo.isLinux) {
                base = new IdeaLaf();
            } else {
                final String name = UIManager.getSystemLookAndFeelClassName();
                base = (BasicLookAndFeel)Class.forName(name).newInstance();
            }
        }
        catch (Exception e) {
            log(e);
        }
    }

    private static Font findFont(String name) {
        for (Font font : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
            if (font.getName().equals(name)) {
                return font;
            }
        }
        return null;
    }

    @Override
    public UIDefaults getDefaults() {
        try {
            final Method superMethod = BasicLookAndFeel.class.getDeclaredMethod("getDefaults");
            superMethod.setAccessible(true);
            final UIDefaults metalDefaults = (UIDefaults)superMethod.invoke(new MetalLookAndFeel());
            final UIDefaults defaults = (UIDefaults)superMethod.invoke(base);
            if (SystemInfo.isLinux && !Registry.is("darcula.use.native.fonts.on.linux")) {
                Font font = findFont("DejaVu Sans");
                if (font != null) {
                    for (Object key : defaults.keySet()) {
                        if (key instanceof String && ((String)key).endsWith(".font")) {
                            defaults.put(key, new FontUIResource(font.deriveFont(13f)));
                        }
                    }
                }
            }

            LafManagerImpl.initInputMapDefaults(defaults);
            initIdeaDefaults(defaults);
            patchStyledEditorKit(defaults);
            patchComboBox(metalDefaults, defaults);
            defaults.remove("Spinner.arrowButtonBorder");
            defaults.put("Spinner.arrowButtonSize", new Dimension(16, 5));
            MetalLookAndFeel.setCurrentTheme(createMetalTheme());
            if (SystemInfo.isWindows) {
                //JFrame.setDefaultLookAndFeelDecorated(true);
            }
            defaults.put("EditorPane.font", defaults.getFont("TextField.font"));
            return defaults;
        }
        catch (Exception e) {
            log(e);
        }
        return super.getDefaults();
    }


    private static void patchComboBox(UIDefaults metalDefaults, UIDefaults defaults) {
        defaults.remove("ComboBox.ancestorInputMap");
        defaults.remove("ComboBox.actionMap");
        defaults.put("ComboBox.ancestorInputMap", metalDefaults.get("ComboBox.ancestorInputMap"));
        defaults.put("ComboBox.actionMap", metalDefaults.get("ComboBox.actionMap"));
    }


    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
    private void patchStyledEditorKit(UIDefaults defaults) {
        URL url = getClass().getResource(getPrefix() + ".css");
        StyleSheet styleSheet = UIUtil.loadStyleSheet(url);
        defaults.put("StyledEditorKit.JBDefaultStyle", styleSheet);
        try {
            Field keyField = HTMLEditorKit.class.getDeclaredField("DEFAULT_STYLES_KEY");
            keyField.setAccessible(true);
            AppContext.getAppContext().put(keyField.get(null), UIUtil.loadStyleSheet(url));
        }
        catch (Exception e) {
            log(e);
        }
    }

    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
    protected void loadDefaults(UIDefaults defaults) {
        final Properties properties = new Properties();
        final String osSuffix = SystemInfo.isMac ? "mac" : SystemInfo.isWindows ? "windows" : "linux";
        try {
            InputStream stream = getClass().getResourceAsStream(getPrefix() + ".properties");
            properties.load(stream);
            stream.close();

            stream = getClass().getResourceAsStream(getPrefix() + "_" + osSuffix + ".properties");
            properties.load(stream);
            stream.close();

            HashMap<String, Object> darculaGlobalSettings = new HashMap<String, Object>();
            final String prefix = getPrefix() + ".";
            for (String key : properties.stringPropertyNames()) {
                if (key.startsWith(prefix)) {
                    darculaGlobalSettings.put(key.substring(prefix.length()), parseValue(key, properties.getProperty(key)));
                }
            }

            for (Object key : defaults.keySet()) {
                if (key instanceof String && ((String)key).contains(".")) {
                    final String s = (String)key;
                    final String darculaKey = s.substring(s.lastIndexOf('.') + 1);
                    if (darculaGlobalSettings.containsKey(darculaKey)) {
                        defaults.put(key, darculaGlobalSettings.get(darculaKey));
                    }
                }
            }

            for (String key : properties.stringPropertyNames()) {
                final String value = properties.getProperty(key);
                defaults.put(key, parseValue(key, value));
            }
        }
        catch (IOException e) {log(e);}
    }
}
