package org.rssowl.ui.internal.dialogs;

import java.util.Collection;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;


/* A dialog to add or edit Labels */
public class LabelDialog extends Dialog {
  private final DialogMode fMode;
  private LocalResourceManager fResources;
  private final ILabel fExistingLabel;
  private Text fNameInput;
  private ToolItem fColorItem;
  private Image fColorImage;
  private RGB fSelectedColor;
  private String fName;
  private Collection<ILabel> fAllLabels;
  
  /* Supported Modes of the Label Dialog */
  public enum DialogMode {

    /** Add Label */
    ADD,

    /** Edit Label */
    EDIT,
  };

  public LabelDialog(Shell parentShell, DialogMode mode, ILabel label) {
    super(parentShell);
    fMode = mode;
    fExistingLabel = label;
    fAllLabels = DynamicDAO.loadAll(ILabel.class);
    fResources = new LocalResourceManager(JFaceResources.getResources());

    if (fExistingLabel != null)
      fSelectedColor = OwlUI.getRGB(fExistingLabel);
    else
      fSelectedColor = new RGB(0, 0, 0);
  }

  public String getName() {
    return fName;
  }

  public RGB getColor() {
    return fSelectedColor;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(LayoutUtils.createGridLayout(2, 10, 10));
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    /* Label */
    Label label = new Label(composite, SWT.None);
    label.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));

    switch (fMode) {
      case ADD:
        label.setText("Please enter the name and color for the new Label:");
        break;
      case EDIT:
        label.setText("Please update the name and color for this Label:");
        break;
    }

    /* Name */
    if (fMode == DialogMode.ADD || fMode == DialogMode.EDIT) {
      Label nameLabel = new Label(composite, SWT.NONE);
      nameLabel.setText("Name: ");

      fNameInput = new Text(composite, SWT.BORDER | SWT.SINGLE);
      fNameInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

      if (fExistingLabel != null) {
        fNameInput.setText(fExistingLabel.getName());
        fNameInput.selectAll();
        fNameInput.setFocus();
      }

      fNameInput.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          onModifyName();
        }
      });
    }

    /* Color */
    if (fMode == DialogMode.ADD || fMode == DialogMode.EDIT) {
      Label nameLabel = new Label(composite, SWT.NONE);
      nameLabel.setText("Color: ");

      Composite colorContainer = new Composite(composite, SWT.BORDER);
      colorContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
      colorContainer.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
      colorContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0));

      ToolBar colorBar = new ToolBar(colorContainer, SWT.FLAT);
      colorBar.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
      colorBar.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));

      fColorItem = new ToolItem(colorBar, SWT.PUSH);
      fColorItem.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          onSelectColor();
        }
      });

      updateColorItem();
    }

    return composite;
  }

  private void onModifyName() {
    boolean labelExists = false;

    for (ILabel label : fAllLabels) {
      if (label.getName().equals(fNameInput.getText()) && label != fExistingLabel) {
        labelExists = true;
        break;
      }
    }

    getButton(IDialogConstants.OK_ID).setEnabled(!labelExists && fNameInput.getText().length() > 0);
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createButtonBar(Composite parent) {

    /* Spacer */
    new Label(parent, SWT.None).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));

    Control control = super.createButtonBar(parent);

    /* Udate enablement */
    getButton(IDialogConstants.OK_ID).setEnabled(fNameInput.getText().length() > 0);

    return control;
  }

  private void updateColorItem() {
    Color color = OwlUI.getColor(fResources, fSelectedColor);

    /* Dispose old first */
    if (fColorImage != null)
      fColorImage.dispose();

    fColorImage = new Image(getShell().getDisplay(), 32, 10);
    GC gc = new GC(fColorImage);

    gc.setBackground(color);
    gc.fillRectangle(0, 0, 32, 10);

    gc.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_BLACK));
    gc.drawRectangle(0, 0, 31, 9);

    gc.dispose();

    fColorItem.setImage(fColorImage);
  }

  private void onSelectColor() {
    ColorDialog dialog = new ColorDialog(getShell());
    dialog.setRGB(fSelectedColor);
    RGB color = dialog.open();
    if (color != null) {
      fSelectedColor = color;
      updateColorItem();
    }
  }

  /*
   * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
   */
  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);

    switch (fMode) {
      case ADD:
        newShell.setText("Add Label");
        break;
      case EDIT:
        newShell.setText("Edit Label");
        break;
    }
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#okPressed()
   */
  @Override
  protected void okPressed() {
    if (fNameInput != null)
      fName = fNameInput.getText();

    super.okPressed();
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#close()
   */
  @Override
  public boolean close() {
    boolean res = super.close();
    if (res && fColorImage != null)
      fColorImage.dispose();

    return res;
  }
}
