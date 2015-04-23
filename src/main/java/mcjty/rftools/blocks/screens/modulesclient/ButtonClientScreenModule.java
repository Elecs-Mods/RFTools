package mcjty.rftools.blocks.screens.modulesclient;

import mcjty.gui.RenderHelper;
import mcjty.gui.events.ButtonEvent;
import mcjty.gui.events.ColorChoiceEvent;
import mcjty.gui.events.TextEvent;
import mcjty.gui.layout.HorizontalLayout;
import mcjty.gui.layout.VerticalLayout;
import mcjty.gui.widgets.*;
import mcjty.rftools.RFTools;
import mcjty.rftools.blocks.screens.ModuleGuiChanged;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class ButtonClientScreenModule implements ClientScreenModule {
    private String line = "";
    private String button = "";
    private boolean toggle = false;
    private int color = 0xffffff;
    private int buttonColor = 0xffffff;
    private boolean activated = false;

    @Override
    public TransformMode getTransformMode() {
        return TransformMode.TEXT;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    @Override
    public void render(FontRenderer fontRenderer, int currenty, Object[] screenData, float factor) {
        GL11.glDisable(GL11.GL_LIGHTING);
        int xoffset;
        if (!line.isEmpty()) {
            fontRenderer.drawString(line, 7, currenty + 2, color);
            xoffset = 7 + 80;
        } else {
            xoffset = 7 + 5;
        }

        boolean act = false;
        if (toggle) {
            if (screenData != null && screenData.length >= 1) {
                act = ((Integer) screenData[0]) > 0;
            }
        } else {
            act = activated;
        }

        RenderHelper.drawBeveledBox(xoffset-5, currenty, 130 - 7, currenty + 12, act ? 0xff333333 : 0xffeeeeee, act ? 0xffeeeeee : 0xff333333, 0xff666666);
        fontRenderer.drawString(fontRenderer.trimStringToWidth(button, 130 - 7 - xoffset), xoffset + (act ? 1 : 0), currenty + 2 + (act ? 1 : 0), buttonColor);
    }

    @Override
    public void mouseClick(World world, int x, int y, boolean clicked) {
        int xoffset;
        if (!line.isEmpty()) {
            xoffset = 80;
        } else {
            xoffset = 5;
        }
        activated = false;
        if (x >= xoffset) {
            activated = clicked;
        }
    }

    @Override
    public Panel createGui(Minecraft mc, Gui gui, final NBTTagCompound currentData, final ModuleGuiChanged moduleGuiChanged) {
        Panel panel = new Panel(mc, gui).setLayout(new VerticalLayout());
        TextField textField = new TextField(mc, gui).setDesiredHeight(16).setTooltips("Label text").addTextEvent(new TextEvent() {
            @Override
            public void textChanged(Widget parent, String newText) {
                currentData.setString("text", newText);
                moduleGuiChanged.updateData();
            }
        });
        panel.addChild(textField);
        TextField buttonTextField = new TextField(mc, gui).setDesiredHeight(16).setTooltips("Button text").addTextEvent(new TextEvent() {
            @Override
            public void textChanged(Widget parent, String newText) {
                currentData.setString("button", newText);
                moduleGuiChanged.updateData();
            }
        });
        panel.addChild(buttonTextField);

        ColorChoiceLabel colorSelector = new ColorChoiceLabel(mc, gui).addColors(0xffffff, 0xff0000, 0x00ff00, 0x0000ff, 0xffff00, 0xff00ff, 0x00ffff).setDesiredWidth(50).setDesiredHeight(14).addChoiceEvent(new ColorChoiceEvent() {
            @Override
            public void choiceChanged(Widget parent, Integer newColor) {
                currentData.setInteger("color", newColor);
                moduleGuiChanged.updateData();
            }
        });
        ColorChoiceLabel buttonColorSelector = new ColorChoiceLabel(mc, gui).addColors(0xffffff, 0xff0000, 0x00ff00, 0x0000ff, 0xffff00, 0xff00ff, 0x00ffff).setDesiredWidth(50).setDesiredHeight(14).addChoiceEvent(new ColorChoiceEvent() {
            @Override
            public void choiceChanged(Widget parent, Integer newColor) {
                currentData.setInteger("buttonColor", newColor);
                moduleGuiChanged.updateData();
            }
        });

        final ToggleButton toggleButton = new ToggleButton(mc, gui).setText("Toggle").setTooltips("Toggle button mode").setDesiredHeight(13).setCheckMarker(true);
        toggleButton.addButtonEvent(new ButtonEvent() {
            @Override
            public void buttonClicked(Widget parent) {
                currentData.setBoolean("toggle", toggleButton.isPressed());
                moduleGuiChanged.updateData();
            }
        });
        panel.addChild(toggleButton);

        if (currentData != null) {
            textField.setText(currentData.getString("text"));
            int currentColor = currentData.getInteger("color");
            if (currentColor != 0) {
                colorSelector.setCurrentColor(currentColor);
            }
            buttonTextField.setText(currentData.getString("button"));
            int currentButtonColor = currentData.getInteger("buttonColor");
            if (currentButtonColor != 0) {
                buttonColorSelector.setCurrentColor(currentButtonColor);
            }
            toggleButton.setPressed(currentData.getBoolean("toggle"));
        }

        panel.addChild(new Panel(mc, gui).setLayout(new HorizontalLayout()).
                addChild(new Label(mc, gui).setText("Color:")).
                addChild(colorSelector).
                setDesiredHeight(18));
        panel.addChild(new Panel(mc, gui).setLayout(new HorizontalLayout()).
                addChild(new Label(mc, gui).setText("Button color:")).
                addChild(buttonColorSelector).
                setDesiredHeight(18));
        return panel;
    }

    @Override
    public void setupFromNBT(NBTTagCompound tagCompound, int dim, int x, int y, int z) {
        if (tagCompound != null) {
            line = tagCompound.getString("text");
            button = tagCompound.getString("button");
            if (tagCompound.hasKey("color")) {
                color = tagCompound.getInteger("color");
            } else {
                color = 0xffffff;
            }
            if (tagCompound.hasKey("buttonColor")) {
                buttonColor = tagCompound.getInteger("buttonColor");
            } else {
                buttonColor = 0xffffff;
            }
            toggle = tagCompound.getBoolean("toggle");
        }
    }

    @Override
    public boolean needsServerData() {
        return true;
    }
}
