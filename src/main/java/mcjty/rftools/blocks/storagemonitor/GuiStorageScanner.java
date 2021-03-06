package mcjty.rftools.blocks.storagemonitor;

import mcjty.lib.container.EmptyContainer;
import mcjty.lib.container.GenericGuiContainer;
import mcjty.lib.entity.SyncedValueList;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.events.ButtonEvent;
import mcjty.lib.gui.events.DefaultSelectionEvent;
import mcjty.lib.gui.events.TextEvent;
import mcjty.lib.gui.events.ValueEvent;
import mcjty.lib.gui.layout.HorizontalAlignment;
import mcjty.lib.gui.layout.HorizontalLayout;
import mcjty.lib.gui.layout.VerticalLayout;
import mcjty.lib.gui.widgets.*;
import mcjty.lib.gui.widgets.Button;
import mcjty.lib.gui.widgets.Label;
import mcjty.lib.gui.widgets.Panel;
import mcjty.lib.gui.widgets.TextField;
import mcjty.lib.network.Argument;
import mcjty.lib.varia.Coordinate;
import mcjty.lib.varia.Logging;
import mcjty.rftools.BlockInfo;
import mcjty.rftools.RFTools;
import mcjty.rftools.network.RFToolsMessages;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class GuiStorageScanner extends GenericGuiContainer<StorageScannerTileEntity> {
    public static final int STORAGE_MONITOR_WIDTH = 256;
    public static final int STORAGE_MONITOR_HEIGHT = 224;

    private WidgetList storageList;
    private WidgetList itemList;
    private EnergyBar energyBar;
    private EnergyBar progressBar;
    private ScrollableLabel radiusLabel;
    private Button scanButton;
    private int clientVersion = -1;

    // For client side: the hilighted coordinates.
    public static Set<Coordinate> fromServer_coordinates = new HashSet<Coordinate>();

    public GuiStorageScanner(StorageScannerTileEntity storageScannerTileEntity, EmptyContainer storageScannerContainer) {
        super(RFTools.instance, RFToolsMessages.INSTANCE, storageScannerTileEntity, storageScannerContainer, RFTools.GUI_MANUAL_MAIN, "stomon");
        storageScannerTileEntity.setCurrentRF(storageScannerTileEntity.getEnergyStored(ForgeDirection.DOWN));

        xSize = STORAGE_MONITOR_WIDTH;
        ySize = STORAGE_MONITOR_HEIGHT;
    }

    @Override
    public void initGui() {
        super.initGui();


        int maxEnergyStored = tileEntity.getMaxEnergyStored(ForgeDirection.DOWN);
        energyBar = new EnergyBar(mc, this).setFilledRectThickness(1).setVertical().setDesiredWidth(8).setDesiredHeight(84).setMaxValue(maxEnergyStored).setShowText(false);
        energyBar.setValue(tileEntity.getCurrentRF());

        storageList = createStyledList().addSelectionEvent(new DefaultSelectionEvent() {
            @Override
            public void select(Widget parent, int index) {
                itemList.removeChildren();
                tileEntity.clearShowingItems();
                getInventoryOnServer();
            }

            @Override
            public void doubleClick(Widget parent, int index) {
                hilightSelectedContainer(index);
            }
        });

        Slider storageListSlider = new Slider(mc, this).setDesiredWidth(10).setVertical().setScrollable(storageList);

        Panel topPanel = new Panel(mc, this).setLayout(new HorizontalLayout().setSpacing(1).setHorizontalMargin(1)).
                setDesiredHeight(90).
                addChild(energyBar).
                addChild(storageList).addChild(storageListSlider);

        itemList = createStyledList();
        Slider itemListSlider = new Slider(mc, this).setDesiredWidth(10).setVertical().setScrollable(itemList);
        Panel midPanel = new Panel(mc, this).setLayout(new HorizontalLayout().setSpacing(1).setHorizontalMargin(1)).
                addChild(itemList).addChild(itemListSlider);

        scanButton = new Button(mc, this).
                setText("Scan").
                setDesiredWidth(50).
                setDesiredHeight(14).
                addButtonEvent(new ButtonEvent() {
                    @Override
                    public void buttonClicked(Widget parent) {
                        startStopScan();
                    }
                }).
                setTooltips("Start/stop a scan of", "all storage units", "in radius");
        progressBar = new EnergyBar(mc, this).setShowText(false).
                setColor1(0xFF777777).setColor2(0xFF555555).
                setHorizontal().setMaxValue(100).setDesiredWidth(30).setValue(0);
        radiusLabel = new ScrollableLabel(mc, this).
                addValueEvent(new ValueEvent() {
                    @Override
                    public void valueChanged(Widget parent, int newValue) {
                        changeRadius(newValue);
                    }
                }).
                setRealMinimum(1).
                setRealMaximum(20).
                setDesiredWidth(30);
        radiusLabel.setRealValue(tileEntity.getRadius());

        TextField textField = new TextField(mc, this).addTextEvent(new TextEvent() {
            @Override
            public void textChanged(Widget parent, String newText) {
                storageList.clearHilightedRows();
                fromServer_coordinates.clear();
                startSearch(newText);
            }
        });
        Panel searchPanel = new Panel(mc, this).setLayout(new HorizontalLayout()).setDesiredHeight(18).addChild(new Label(mc, this).setText("Search:")).addChild(textField);

        Slider radiusSlider = new Slider(mc, this).
                setHorizontal().
                setTooltips("Radius of scan").
                setScrollable(radiusLabel);
        Panel scanPanel = new Panel(mc, this).setLayout(new HorizontalLayout()).setDesiredHeight(18).addChild(scanButton).addChild(progressBar).addChild(radiusSlider).addChild(radiusLabel);

        Widget toplevel = new Panel(mc, this).setFilledRectThickness(2).setLayout(new VerticalLayout().setSpacing(1).setVerticalMargin(3)).addChild(topPanel).addChild(midPanel).addChild(searchPanel).addChild(scanPanel);
        toplevel.setBounds(new Rectangle(guiLeft, guiTop, xSize, ySize));

        window = new Window(this, toplevel);

        Keyboard.enableRepeatEvents(true);

        tileEntity.requestRfFromServer(RFToolsMessages.INSTANCE);
    }

    private void hilightSelectedContainer(int index) {
        if (index == -1) {
            return;
        }
        SyncedValueList<InvBlockInfo> inventories = tileEntity.getInventories();
        Coordinate c = inventories.get(index).getCoordinate();
        RFTools.instance.clientInfo.hilightBlock(c, System.currentTimeMillis()+1000* StorageScannerConfiguration.hilightTime);
        Logging.message(mc.thePlayer, "The inventory is now highlighted");
        mc.getMinecraft().thePlayer.closeScreen();
    }

    private void changeRadius(int r) {
        sendServerCommand(RFToolsMessages.INSTANCE, StorageScannerTileEntity.CMD_SETRADIUS, new Argument("r", r));
    }

    private void startStopScan() {
        sendServerCommand(RFToolsMessages.INSTANCE, StorageScannerTileEntity.CMD_STARTSCAN, new Argument("start", !tileEntity.isScanning()));
    }

    private void startSearch(String text) {
        if (!text.isEmpty()) {
            RFToolsMessages.INSTANCE.sendToServer(new PacketSearchItems(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, text));
        }
    }

    private void getInventoryOnServer() {
        InvBlockInfo invBlockInfo = getSelectedContainer();
        if (invBlockInfo != null) {
            Coordinate c = invBlockInfo.getCoordinate();
            RFToolsMessages.INSTANCE.sendToServer(new PacketGetInventory(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord,
                    c.getX(), c.getY(), c.getZ()));
        }
    }

    private InvBlockInfo getSelectedContainer() {
        int selected = storageList.getSelected();
        if (selected != -1) {
            SyncedValueList<InvBlockInfo> inventories = tileEntity.getInventories();
            if (selected < inventories.size()) {
                InvBlockInfo invBlockInfo = inventories.get(selected);
                return invBlockInfo;
            }
        }
        return null;
    }

    private void updateContentsList() {
        List<ItemStack> items = tileEntity.getShowingItems();
        if (itemList.getMaximum() == 0) {
            // We need to refresh.
            for (ItemStack stack : items) {
                if (stack != null) {
                    String displayName = BlockInfo.getReadableName(stack, 0);

                    Panel panel = new Panel(mc, this).setLayout(new HorizontalLayout());
                    panel.addChild(new BlockRender(mc, this).setRenderItem(stack));
                    panel.addChild(new Label(mc, this).setDynamic(true).setText(displayName).setHorizontalAlignment(HorizontalAlignment.ALIGH_LEFT));
                    itemList.addChild(panel);
                }
            }
        }
    }

    private void updateStorageList() {
        SyncedValueList<InvBlockInfo> inventories = tileEntity.getInventories();
        if (inventories.getClientVersion() != clientVersion) {
            clientVersion = inventories.getClientVersion();
            storageList.removeChildren();
            for (InvBlockInfo blockInfo : inventories) {
                Coordinate c = blockInfo.getCoordinate();
                Block block = mc.theWorld.getBlock(c.getX(), c.getY(), c.getZ());
                int meta = mc.theWorld.getBlockMetadata(c.getX(), c.getY(), c.getZ());
                String displayName;
                if (block == null || block.isAir(mc.theWorld, c.getX(), c.getY(), c.getZ())) {
                    displayName = "[REMOVED]";
                    block = null;
                } else {
                    displayName = BlockInfo.getReadableName(block, meta);
                }

                Panel panel = new Panel(mc, this).setLayout(new HorizontalLayout());
                panel.addChild(new BlockRender(mc, this).setRenderItem(block));
                panel.addChild(new Label(mc, this).setText(displayName).setHorizontalAlignment(HorizontalAlignment.ALIGH_LEFT).setDesiredWidth(90));
                panel.addChild(new Label(mc, this).setDynamic(true).setText(c.toString()));
                storageList.addChild(panel);
            }
        }
        storageList.clearHilightedRows();
        Set<Coordinate> coordinates = fromServer_coordinates;
        int i = 0;
        for (InvBlockInfo blockInfo : inventories) {
            Coordinate c = blockInfo.getCoordinate();
            if (coordinates.contains(c)) {
                storageList.addHilightedRow(i);
            }
            i++;
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float v, int i, int i2) {
        updateStorageList();
        updateContentsList();
        updateScanButton();
        drawWindow();
        int currentRF = tileEntity.getCurrentRF();
        energyBar.setValue(currentRF);
        tileEntity.requestRfFromServer(RFToolsMessages.INSTANCE);
    }

    private void updateScanButton() {
        if (tileEntity.isScanning()) {
            scanButton.setText("Stop");
            progressBar.setValue(tileEntity.getProgress());
        } else {
            scanButton.setText("Scan");
            progressBar.setValue(0);
        }
    }
}
