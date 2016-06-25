package com.techno_wizard.mcguicreator.management;

import com.techno_wizard.mcguicreator.gui.*;
import com.techno_wizard.mcguicreator.gui.events.AutoGenerateType;
import com.techno_wizard.mcguicreator.gui.inventory.*;
import com.techno_wizard.mcguicreator.help.*;
import org.bukkit.entity.Item;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Ethan on 4/6/2016.
 */
public class EditorManager {

    private MainMenu mainMenu;
    private JEditorPane stackNameEditor;
    private JCheckBox showFormattedTxtDetails;
    private JCheckBox showFormattedTxtLore;
    private JSpinner stackItemCountSpinner;
    private JComboBox materialComboBox;
    private JTextField notesBox;
    private JEditorPane loreEditor;
    private JCheckBox showFormattedTxtInv;
    private JEditorPane inventoryNameEditor;
    private JComboBox eventGeneratorBox;
    private JSpinner inventorySizeSpinner;
    private JRadioButton closeInvOnClick;

    private JList enchantmentList;

    private boolean textIsFormatted;
    private String stackNamePlain = "";
    private String inventoryNamePlain = "";
    private String lorePlain = "";

    private TextEditorManager textEditorManager;


    public EditorManager(MainMenu mainMenu, JEditorPane stackNameEditor, JCheckBox showFormattedTxtDetails,
                         JCheckBox showFormattedTxtLore, JCheckBox showFormattedTxtInv, JSpinner stackItemCountSpinner, JComboBox materialBox,
                         JTextField notesBox, JEditorPane loreEditor,
                         JTabbedPane editorTabbedPane, JEditorPane inventoryNameEditor,
                         JComboBox eventGeneratorBox, JSpinner inventorySizeSpinner,
                         JList enchantmentList, JRadioButton closeInvOnClick) {
        this.inventoryNameEditor = inventoryNameEditor;
        this.mainMenu = mainMenu;
        this.stackNameEditor = stackNameEditor;
        this.showFormattedTxtDetails = showFormattedTxtDetails;
        this.showFormattedTxtLore = showFormattedTxtLore;
        this.stackItemCountSpinner = stackItemCountSpinner;
        this.materialComboBox = materialBox;
        this.notesBox = notesBox;
        this.loreEditor = loreEditor;
        this.showFormattedTxtInv = showFormattedTxtInv;
        JTabbedPane editorTabbedPane1 = editorTabbedPane;
        this.eventGeneratorBox = eventGeneratorBox;
        this.inventorySizeSpinner = inventorySizeSpinner;
        this.closeInvOnClick = closeInvOnClick;

        this.enchantmentList = enchantmentList;


        this.textEditorManager = new TextEditorManager(this);

        initEditors();
    }

    public void initEditors() {
        //TODO: Find a better way to stop useres from creating new lines
        this.inventoryNameEditor.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                inventoryNameEditor.setText(inventoryNameEditor.getText().replace("\n"," "));
            }
        });
        this.stackNameEditor.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e){
                stackNameEditor.setText(stackNameEditor.getText().replace("\n"," "));
            }
        });

        materialComboBox.addActionListener(e ->
                mainMenu.getInvManager().updateActiveItemStackIcon((Material) materialComboBox.getSelectedItem()));
        //TODO add text formatting switch
        ActionListener listener = e1 -> setTextIsFormatted(((JCheckBox) e1.getSource()).isSelected());
        showFormattedTxtLore.addActionListener(listener);
        showFormattedTxtDetails.addActionListener(listener);
        showFormattedTxtInv.addActionListener(listener);

        inventorySizeSpinner.setValue(mainMenu.getInvManager().getInventoryTableModel().getRowCount());
        inventorySizeSpinner.addChangeListener(e -> {
            if ((int) inventorySizeSpinner.getValue() < mainMenu.getInventoryTable().getRowCount()) {
                boolean isDeletingItemstacks = false;
                if ((int) inventorySizeSpinner.getValue() > 6) {
                    inventorySizeSpinner.setValue(6);
                } else if ((int) inventorySizeSpinner.getValue() > 0) {
                    for (int rows = (int) inventorySizeSpinner.getValue(); rows < mainMenu.getInventoryTable().getRowCount(); rows++) {
                        if (mainMenu.getInvManager().getInventoryTableModel().rowContainsItemstacks(rows)) {
                            WarningPopUp wpu = new WarningPopUp("A row you are deleting contains an itemstack.", new WarningResult() {
                                @Override
                                public void onActivate() {
                                    updateInventorySize((int) inventorySizeSpinner.getValue());
                                }

                                @Override
                                public void onCancel() {
                                    inventorySizeSpinner.setValue(mainMenu.getInvManager().getInventoryTableModel().getRowCount());
                                }
                            });
                            isDeletingItemstacks = true;
                            break;
                        }
                    }
                    if (!isDeletingItemstacks)
                        updateInventorySize((int) inventorySizeSpinner.getValue());
                } else
                    inventorySizeSpinner.setValue(1);
            } else
                updateInventorySize((int) inventorySizeSpinner.getValue());

        });


        //Set the font for all the text fields
        stackNameEditor.setFont(ItemUtil.getMCFont(stackNameEditor.getFont()));
        loreEditor.setFont(ItemUtil.getMCFont(loreEditor.getFont()));
        inventoryNameEditor.setFont(ItemUtil.getMCFont(inventoryNameEditor.getFont()));
        enchantmentList.setFont(ItemUtil.getMCFont(enchantmentList.getFont()));
    }

    public void loadStack(ItemStack stack) {
        stackNameEditor.setText(stack.getName());
        materialComboBox.setSelectedItem(stack.getMaterial());

        //todo render if showFormatted is true
        loreEditor.setText(stack.getLore());
        materialComboBox.setSelectedItem(stack.getMaterial());
        stackItemCountSpinner.setValue(stack.getAmount());
        notesBox.setText(stack.getNotes());
        closeInvOnClick.setSelected(stack.getCloseInvOnClick());
        ((DefaultListModel) enchantmentList.getModel()).clear();

        for (int i = 0; i < eventGeneratorBox.getItemCount(); i++) {
            if (AutoGenerateType.getTypeByName((String) eventGeneratorBox.getItemAt(i)) == stack.getAutoGenerateType()) {
                this.eventGeneratorBox.setSelectedIndex(i);
                break;
            }
        }
        for (Enchantment e : stack.getEnchantments())
            ((DefaultListModel) enchantmentList.getModel()).addElement(e.getDisplay());
    }

    public void saveCurrentItemStack() {
        this.saveItemStack(mainMenu.getInvManager().getActiveItemStack());
    }

    public void saveItemStack(ItemStack is) {
        ItemStack oldItemstack = is;
        //Make sure the itemstack exists;
        if (oldItemstack == null)
            return;

        //todo revert to unformatted version first
        setTextIsFormatted(false);
        oldItemstack.setName(stackNameEditor.getText());
        oldItemstack.setMaterial((Material) materialComboBox.getSelectedItem());
        oldItemstack.setLore(loreEditor.getText());
        oldItemstack.setAmount((Integer) stackItemCountSpinner.getValue());
        oldItemstack.setNotes(notesBox.getText());
        oldItemstack.setAutoGenerateType(AutoGenerateType.getTypeByName((String) (eventGeneratorBox.getSelectedItem())));
        oldItemstack.setCloseInvOnClick(closeInvOnClick.isSelected());
        List<Enchantment> enchs = new ArrayList<>();
        for (int i = 0; i < ((DefaultListModel) enchantmentList.getModel()).size(); i++) {
            String s = (String) enchantmentList.getModel().getElementAt(i);
            Enchantment ench = new Enchantment(Enchantment.EnchantmentType.getEnchantmentByName(s.split(" : ")[0]), ItemUtil.getIntegers(s.split(" : ")[1]));
            enchs.add(ench);
        }
        oldItemstack.setEnchantments(enchs);
    }

    /**
     * sets the selected editor view
     *
     * @param textIsFormatted whether or not the text should be formatted
     */
    public void setTextIsFormatted(boolean textIsFormatted) {
        //todo saving when text is formatted will result in incorrect data saving.
        if ((this.textIsFormatted && textIsFormatted) || (!this.textIsFormatted && !textIsFormatted)) return;

        if (!textIsFormatted) {
            // set text unformatted
            inventoryNameEditor.setEditable(true);
            inventoryNameEditor.setContentType("text/plain");
            inventoryNameEditor.setText(inventoryNamePlain);

            loreEditor.setEditable(true);
            loreEditor.setContentType("text/plain");
            loreEditor.setText(lorePlain);

            stackNameEditor.setEditable(true);
            stackNameEditor.setContentType("text/plain");
            stackNameEditor.setText(stackNamePlain);

            // set box color
            inventoryNameEditor.setBackground(Color.WHITE);
            loreEditor.setBackground(Color.WHITE);
            stackNameEditor.setBackground(Color.WHITE);
        } else {
            inventoryNamePlain = inventoryNameEditor.getText();
            inventoryNameEditor.setContentType("text/html");
            inventoryNameEditor.setEditable(false);
            inventoryNameEditor.setText(ChatColor.getFormattedColorText(inventoryNamePlain));

            lorePlain = loreEditor.getText();
            loreEditor.setContentType("text/html");
            loreEditor.setEditable(false);
            loreEditor.setText(ChatColor.getFormattedColorText(lorePlain));

            stackNamePlain = stackNameEditor.getText();
            stackNameEditor.setContentType("text/html");
            stackNameEditor.setEditable(false);
            stackNameEditor.setText(ChatColor.getFormattedColorText(stackNamePlain));

            // set color for readability with white
            inventoryNameEditor.setBackground(Color.DARK_GRAY);
            loreEditor.setBackground(Color.DARK_GRAY);
            stackNameEditor.setBackground(Color.DARK_GRAY);

        }

        this.textIsFormatted = textIsFormatted;
        showFormattedTxtDetails.setSelected(textIsFormatted);
        showFormattedTxtInv.setSelected(textIsFormatted);
        showFormattedTxtLore.setSelected(textIsFormatted);
    }

    public JEditorPane getInventoryNameEditor() {
        return inventoryNameEditor;
    }

    public JEditorPane getItemStackNameEditor() {
        return stackNameEditor;
    }

    public JEditorPane getItemStackLoreEditor() {
        return loreEditor;
    }

    public JTextField getNotes() {
        return notesBox;
    }
    public TextEditorManager getTextEditorManager() {
        return textEditorManager;
    }

    public void updateInventorySize(int size) {
        mainMenu.getInvManager().getInventoryTableModel().setInventorySize(size);
        mainMenu.getInventoryTable().setRowHeight(75);
        mainMenu.getInventoryTable().setModel(mainMenu.getInvManager().getInventoryTableModel());
    }

    public void reloadData(InventoryTableModel model, ItemStack selected) {
        setTextIsFormatted(false);
        inventoryNameEditor.setText(model.getInventoryName());
        stackNameEditor.setText(selected.getName());
        notesBox.setText(selected.getNotes());

        // to prevent casting issues
        DefaultListModel enchModel = new DefaultListModel();
        enchModel.setSize(selected.getEnchantments().size());
        for (int i = 0; i < selected.getEnchantments().size(); i++) {
            enchModel.set(i, selected.getEnchantments().get(i));
        }

        enchantmentList.setModel(enchModel);
        stackItemCountSpinner.setValue(selected.getAmount());
        loreEditor.setText(selected.getLore());
        eventGeneratorBox.setSelectedItem(selected.getAutoGenerateType());
        inventorySizeSpinner.setValue(model.getRowCount());
        closeInvOnClick.setSelected(selected.getCloseInvOnClick());
        materialComboBox.setSelectedItem(selected.getMaterial());
    }
}
