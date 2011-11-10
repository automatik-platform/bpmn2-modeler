/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 *  All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 *
 * @author Bob Brodt
 ******************************************************************************/

package org.eclipse.bpmn2.modeler.ui.property;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.Bpmn2Factory;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.modeler.core.ModelHandler;
import org.eclipse.bpmn2.modeler.core.ModelHandlerLocator;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.core.utils.PropertyUtil;
import org.eclipse.bpmn2.modeler.ui.Activator;
import org.eclipse.bpmn2.modeler.ui.editor.BPMN2Editor;
import org.eclipse.bpmn2.modeler.ui.property.providers.ColumnTableProvider;
import org.eclipse.bpmn2.modeler.ui.property.providers.TableCursor;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.BasicFeatureMap;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.FeatureMap.Entry;
import org.eclipse.emf.edit.provider.ItemProviderAdapter;
import org.eclipse.emf.edit.ui.provider.PropertyDescriptor.EDataTypeCellEditor;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.ResourceSetChangeEvent;
import org.eclipse.emf.transaction.ResourceSetListenerImpl;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.impl.TransactionChangeRecorder;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
 

/**
 * @author Bob Brodt
 *
 */
public class AbstractBpmn2TableComposite extends Composite {

	public static final Bpmn2Factory MODEL_FACTORY = Bpmn2Factory.eINSTANCE;
	
	public static final int HIDE_TITLE = 1 << 18; // Hide section title - useful if this is the only thing in the PropertySheetTab
	public static final int ADD_BUTTON = 1 << 19; // show "Add" button
	public static final int REMOVE_BUTTON = 1 << 20; // show "Remove" button
	public static final int MOVE_BUTTONS = 1 << 21; // show "Up" and "Down" buttons
	public static final int EDIT_BUTTON = 1 << 23; // show "Edit..." button
	public static final int SHOW_DETAILS = 1 << 24; // create a "Details" section
	public static final int DEFAULT_STYLE = (
			ADD_BUTTON|REMOVE_BUTTON|MOVE_BUTTONS|SHOW_DETAILS);
	
	public static final int CUSTOM_STYLES_MASK = (
			HIDE_TITLE|ADD_BUTTON|REMOVE_BUTTON|MOVE_BUTTONS|EDIT_BUTTON|SHOW_DETAILS);
	public static final int CUSTOM_BUTTONS_MASK = (
			ADD_BUTTON|REMOVE_BUTTON|MOVE_BUTTONS|EDIT_BUTTON);

	protected AbstractBpmn2PropertySection propertySection;
	protected FormToolkit toolkit;
	protected BPMN2Editor bpmn2Editor;
	protected IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();

	protected EClass listItemClass;
	
	// widgets
	SashForm sashForm;
	Section tableSection;
	Section detailSection;
	
	Table table;
	TableViewer tableViewer;
	
	Composite tableAndButtonsComposite;
	Composite buttonsComposite;
	Composite tableComposite;
	AbstractBpmn2PropertiesComposite detailComposite;
	
	Button addButton;
	Button removeButton;
	Button upButton;
	Button downButton;
	Button editButton;

	protected int style;
	
	protected AbstractTableProvider tableProvider;
	
	public AbstractBpmn2TableComposite(AbstractBpmn2PropertySection section, int style) {
		super(section.getSectionRoot(), style & ~CUSTOM_STYLES_MASK);

		propertySection = section;
		toolkit = propertySection.getWidgetFactory();
		initialize(style);
	}

	public AbstractBpmn2TableComposite(final Composite parent, int style) {
		super(parent, style & ~CUSTOM_STYLES_MASK);

		toolkit = new FormToolkit(Display.getCurrent());
		initialize(style);
	}
	
	private void initialize(int style) {
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				PropertyUtil.disposeChildWidgets(AbstractBpmn2TableComposite.this);
			}
		});

		// assume we are being placed in an AbstractBpmn2PropertyComposite which has
		// a GridLayout of 3 columns
		setLayout(new GridLayout(3, false));
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		this.style = style;
		toolkit.adapt(this);
		toolkit.paintBordersFor(this);
	}
	
	public TabbedPropertySheetPage getTabbedPropertySheetPage() {
		return propertySection.getTabbedPropertySheetPage();
	}
	
	public void setTableProvider(AbstractTableProvider provider) {
		tableProvider = provider;
	}
	
	public void setListItemClass(EClass clazz) {
		this.listItemClass = clazz;
	}
	
	public EClass getListItemClass(EObject object, EStructuralFeature feature) {
		if (listItemClass!=null)
			return listItemClass;
		return (EClass) feature.getEType();
		
	}
	
	/**
	 * Find all subtypes of the given listItemClass EClass and display a selection
	 * list if there are more than 1 subtypes.
	 * 
	 * @param listItemClass
	 * @return
	 */
	public EClass getListItemClassToAdd(EClass listItemClass) {
		final List<EClass> items = new ArrayList<EClass>();
		for (EClassifier eclassifier : Bpmn2Package.eINSTANCE.getEClassifiers() ) {
			if (eclassifier instanceof EClass) {
				EClass eclass = (EClass)eclassifier;
				if (eclass.getEAllSuperTypes().contains(listItemClass)) {
					items.add(eclass);
				}
			}
		}
		if (items.size()>1) {
			ListDialog dialog = new ListDialog(getShell());
			dialog.setContentProvider(new IStructuredContentProvider() {
	
				@Override
				public void dispose() {
				}
	
				@Override
				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				}
	
				@Override
				public Object[] getElements(Object inputElement) {
					return items.toArray();
				}
				
			});
			dialog.setLabelProvider(new ILabelProvider() {
	
				@Override
				public void addListener(ILabelProviderListener listener) {
				}
	
				@Override
				public void dispose() {
				}
	
				@Override
				public boolean isLabelProperty(Object element, String property) {
					return false;
				}
	
				@Override
				public void removeListener(ILabelProviderListener listener) {
				}
	
				@Override
				public Image getImage(Object element) {
					return null;
				}
	
				@Override
				public String getText(Object element) {
					return ModelUtil.toDisplayName( ((EClass)element).getName() );
				}
				
			});
			dialog.setTitle("Select a type of "+
					ModelUtil.toDisplayName(listItemClass.getName()));
			dialog.setAddCancelButton(true);
			dialog.setHelpAvailable(false);
			dialog.setInput(new Object());
			dialog.open();
			Object[] result = dialog.getResult();
			if (result != null) {
				return (EClass)result[0];
			}
			return null;
		}
		return listItemClass;
	}
	
	/**
	 * Create a default ColumnTableProvider if none was set in setTableProvider();
	 * @param object
	 * @param feature
	 * @return
	 */
	public AbstractTableProvider getTableProvider(EObject object, EStructuralFeature feature) {
		if (tableProvider==null) {
			final EList<EObject> list = (EList<EObject>)object.eGet(feature);
			final EClass listItemClass = getListItemClass(object, feature);

			tableProvider = new AbstractTableProvider() {
				@Override
				public boolean canModify(EObject object, EStructuralFeature feature, EObject item) {
					return false;
				}
			};
			
			for (EAttribute a1 : listItemClass.getEAllAttributes()) {
				if ("anyAttribute".equals(a1.getName())) {
					List<EStructuralFeature> anyAttributes = new ArrayList<EStructuralFeature>();
					// are there any actual "anyAttribute" instances we can look at
					// to get the feature names and types from?
					// TODO: enhance the table to dynamically allow creation of new
					// columns which will be added to the "anyAttributes"
					for (EObject instance : list) {
						Object o = instance.eGet(a1);
						if (o instanceof BasicFeatureMap) {
							BasicFeatureMap map = (BasicFeatureMap)o;
							for (Entry entry : map) {
								EStructuralFeature f1 = entry.getEStructuralFeature();
								if (f1 instanceof EAttribute && !anyAttributes.contains(f1)) {
									tableProvider.add(new TableColumn(object,(EAttribute)f1));
									anyAttributes.add(f1);
								}
							}
						}
					}
				}
				else if (FeatureMap.Entry.class.equals(a1.getEType().getInstanceClass())) {
					// TODO: how do we handle these?
					if (a1 instanceof EAttribute)
						tableProvider.add(new TableColumn(object,a1));
					else
						System.out.println("FeatureMapEntry: "+listItemClass.getName()+"."+a1.getName());
				}
				else {
					tableProvider.add(new TableColumn(object,a1));
				}
			}
		}
		return tableProvider;
	}
	
	/**
	 * Override this to create your own Details section. This composite will be displayed
	 * in a twistie section whenever the user selects an item from the table. The section
	 * is automatically hidden when the table is collapsed.
	 * 
	 * @param object
	 * @return
	 */
	public AbstractBpmn2PropertiesComposite createDetailComposite(Composite parent, Class eClass) {
		AbstractBpmn2PropertiesComposite composite = PropertiesCompositeFactory.createComposite(eClass, parent, SWT.NONE);
		return composite;
	}
	
	/**
	 * Override this if construction of new list items needs special handling. 
	 * @param object
	 * @param feature
	 * @return
	 */
	protected EObject addListItem(EObject object, EStructuralFeature feature) {
		EList<EObject> list = (EList<EObject>)object.eGet(feature);
		EClass listItemClass = getListItemClass(object,feature);
		EObject newItem = null;
		if (!(list instanceof EObjectContainmentEList)) {
			// this is not a containment list so we can't add it
			// because we don't know where the new object belongs
			
			MessageDialog.openError(getShell(), "Internal Error",
					"Can not create a new " +
					listItemClass.getName() +
					" because the list is not a container. " +
					"The default addListItem() method must be implemented."
			);
			return null;
		}
		else {
			try {
				ModelHandler modelHandler = ModelHandlerLocator.getModelHandler(object.eResource());
				if (this.listItemClass==null) {
					listItemClass = getListItemClassToAdd(listItemClass);
					if (listItemClass==null)
						return null; // user cancelled
				}
				newItem = modelHandler.create(listItemClass);
				list.add(newItem);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return newItem;
	}

	/**
	 * Override this if editing of new list items needs special handling. 
	 * @param object
	 * @param feature
	 * @return
	 */
	protected EObject editListItem(EObject object, EStructuralFeature feature) {
		return null;
	}	
	/**
	 * Override this if removal of list items needs special handling. 
	 * @param object
	 * @param feature
	 * @param item
	 * @return
	 */
	protected boolean removeListItem(EObject object, EStructuralFeature feature, Object item) {
		EList<EObject> list = (EList<EObject>)object.eGet(feature);
		list.remove(item);
		return true;
	}

	public void bindList(final EObject object, final EStructuralFeature feature) {
		if (!(object.eGet(feature) instanceof EList<?>)) {
			return;
		}
		Class<?> clazz = feature.getEType().getInstanceClass();
		if (!EObject.class.isAssignableFrom(clazz)) {
			return;
		}
		TransactionChangeRecorder cr = null;
		TransactionalEditingDomain dom = null;
		for (Adapter ad : object.eAdapters()) {
			if (ad instanceof TransactionChangeRecorder) {
				cr = (TransactionChangeRecorder)ad;
				dom = cr.getEditingDomain();
			}
		}

		if (bpmn2Editor==null)
			bpmn2Editor = BPMN2Editor.getEditor(object);
		if (bpmn2Editor==null)
			return;
		
		final TransactionalEditingDomain editingDomain = bpmn2Editor.getEditingDomain();
		final EList<EObject> list = (EList<EObject>)object.eGet(feature);
		final EClass listItemClass = getListItemClass(object,feature);
		
		////////////////////////////////////////////////////////////
		// Collect columns to be displayed and build column provider
		////////////////////////////////////////////////////////////
		tableProvider = getTableProvider(object, feature);
		if (tableProvider.getColumns().size()==0) {
			return;
		}

		////////////////////////////////////////////////////////////
		// SashForm contains the table section and a possible
		// details section
		////////////////////////////////////////////////////////////
		if ((style & HIDE_TITLE)==0 || (style & SHOW_DETAILS)!=0) {
			// display title in the table section and/or show a details section
			// SHOW_DETAILS forces drawing of a section title
			sashForm = new SashForm(this, SWT.NONE);
			sashForm.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));
			
			tableSection = toolkit.createSection(sashForm,
					ExpandableComposite.TWISTIE |
					ExpandableComposite.COMPACT |
					ExpandableComposite.TITLE_BAR);
			tableSection.setText(ModelUtil.toDisplayName(listItemClass.getName())+" List");

			tableComposite = toolkit.createComposite(tableSection, SWT.NONE);
			tableSection.setClient(tableComposite);
			tableComposite.setLayout(new GridLayout(3, false));
			createTableAndButtons(tableComposite,style);
			
			detailSection = toolkit.createSection(sashForm,
					ExpandableComposite.TWISTIE |
					ExpandableComposite.EXPANDED |
					ExpandableComposite.TITLE_BAR);
			detailSection.setText(ModelUtil.toDisplayName(listItemClass.getName()) + " Details");

			detailComposite = createDetailComposite(detailSection, listItemClass.getInstanceClass());
			if (detailComposite!=null) {
				detailSection.setClient(detailComposite);
				toolkit.adapt(detailComposite);
//				detailComposite.setPropertySection(propertySection);
			}
			detailSection.setVisible(false);

			tableSection.addExpansionListener(new IExpansionListener() {

				@Override
				public void expansionStateChanging(ExpansionEvent e) {
					if (!e.getState() && detailSection!=null) {
						detailSection.setVisible(false);
					}
				}

				@Override
				public void expansionStateChanged(ExpansionEvent e) {
					preferenceStore.setValue("table."+listItemClass.getName()+".expanded", e.getState());
					redrawPage();
				}
				
			});
			
			if (detailSection!=null) {
				detailSection.addExpansionListener(new IExpansionListener() {
	
					@Override
					public void expansionStateChanging(ExpansionEvent e) {
						if (!e.getState()) {
							detailSection.setVisible(false);
						}
					}
	
					@Override
					public void expansionStateChanged(ExpansionEvent e) {
						redrawPage();
					}
				});
			}					
			
			sashForm.setWeights(new int[] { 1, 2 });
		}
		else {
			createTableAndButtons(this,style);
		}
		
		////////////////////////////////////////////////////////////
		// Create table viewer and cell editors
		////////////////////////////////////////////////////////////
		tableViewer = new TableViewer(table);
		tableProvider.createTableLayout(table);
		tableProvider.setTableViewer(tableViewer);
		
		tableViewer.setLabelProvider(tableProvider);
		tableViewer.setCellModifier(tableProvider);
		tableViewer.setContentProvider(new ContentProvider(object, feature, list));
		tableViewer.setColumnProperties(tableProvider.getColumnProperties());
		tableViewer.setCellEditors(tableProvider.createCellEditors(table));

		////////////////////////////////////////////////////////////
		// add a resource set listener to update the treeviewer when
		// when something interesting happens
		////////////////////////////////////////////////////////////
		final ResourceSetListenerImpl domainListener = new ResourceSetListenerImpl() {
			@Override
			public void resourceSetChanged(ResourceSetChangeEvent event) {
				List<Notification> notifications = event.getNotifications();
				try {
					for (Notification notification : notifications) {
						tableViewer.setInput(list);
					}
				}
				catch (Exception e) {
					// silently ignore :-o
				}
			}
		};
		editingDomain.addResourceSetListener(domainListener);
		table.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				editingDomain.removeResourceSetListener(domainListener);
			}
		});

		////////////////////////////////////////////////////////////
		// Create handlers
		////////////////////////////////////////////////////////////
		tableViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				boolean enable = !event.getSelection().isEmpty();
				if (detailSection!=null) {
					detailSection.setVisible(enable);
					if (enable) {
						IStructuredSelection sel = (IStructuredSelection) event.getSelection();
						if (sel.getFirstElement() instanceof EObject && detailComposite instanceof AbstractBpmn2PropertiesComposite) {
							EObject o = (EObject)sel.getFirstElement();
							((AbstractBpmn2PropertiesComposite)detailComposite).setEObject(bpmn2Editor,o);
							detailSection.setText(ModelUtil.toDisplayName(o.eClass().getName()) + " Details");
						}
						detailSection.setExpanded(true);
					}
					sashForm.layout(true);
					redrawPage();
				}
				if (removeButton!=null)
					removeButton.setEnabled(enable);
				if (editButton!=null)
					editButton.setEnabled(enable);
				if (upButton!=null && downButton!=null) {
					int i = table.getSelectionIndex();
					if (i>0)
						upButton.setEnabled(enable);
					else
						upButton.setEnabled(false);
					if (i<table.getItemCount()-1)
						downButton.setEnabled(enable);
					else
						downButton.setEnabled(false);
				}
			}
		});
		
		if (addButton!=null) {
			addButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					editingDomain.getCommandStack().execute(new RecordingCommand(editingDomain) {
						@Override
						protected void doExecute() {
							EObject newItem = addListItem(object,feature);
							if (newItem!=null) {
								tableViewer.setInput(list);
								tableViewer.setSelection(new StructuredSelection(newItem));
							}
						}
					});
				}
			});
		}

		if (removeButton!=null) {
			removeButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					editingDomain.getCommandStack().execute(new RecordingCommand(editingDomain) {
						@Override
						protected void doExecute() {
							int i = table.getSelectionIndex();
							if (removeListItem(object,feature,list.get(i))) {
								tableViewer.setInput(list);
								if (i>=list.size())
									i = list.size() - 1;
								if (i>=0)
									tableViewer.setSelection(new StructuredSelection(list.get(i)));
							}
						}
					});
				}
			});
		}

		if (upButton!=null && downButton!=null) {
			upButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					editingDomain.getCommandStack().execute(new RecordingCommand(editingDomain) {
						@Override
						protected void doExecute() {
							int i = table.getSelectionIndex();
							list.move(i-1, i);
							tableViewer.setInput(list);
							tableViewer.setSelection(new StructuredSelection(list.get(i-1)));
						}
					});
				}
			});
			
			downButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					editingDomain.getCommandStack().execute(new RecordingCommand(editingDomain) {
						@Override
						protected void doExecute() {
							int i = table.getSelectionIndex();
							list.move(i+1, i);
							tableViewer.setInput(list);
							tableViewer.setSelection(new StructuredSelection(list.get(i+1)));
						}
					});
				}
			});
		}

		if (editButton!=null) {
			editButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					editingDomain.getCommandStack().execute(new RecordingCommand(editingDomain) {
						@Override
						protected void doExecute() {
							EObject newItem = editListItem(object,feature);
							if (newItem!=null) {
								tableViewer.setInput(list);
								tableViewer.setSelection(new StructuredSelection(newItem));
							}
						}
					});
				}
			});
		}
		
		tableViewer.setInput(list);
		
		// a TableCursor allows navigation of the table with keys
		TableCursor.create(table, tableViewer);
		redrawPage();
		
		boolean expanded = preferenceStore.getBoolean("table."+listItemClass.getName()+".expanded");
		if (expanded)
			tableSection.setExpanded(true);
	}
	
	protected void redrawPage() {
		if (propertySection!=null)
			propertySection.layout();
		else
			getParent().layout();
	}

	private void createTableAndButtons(Composite parent, int style) {

		GridData gridData;
		
		////////////////////////////////////////////////////////////
		// Create a composite to hold the buttons and table
		////////////////////////////////////////////////////////////
		tableAndButtonsComposite = toolkit.createComposite(parent, SWT.NONE);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		tableAndButtonsComposite.setLayoutData(gridData);
		tableAndButtonsComposite.setLayout(new GridLayout(2, false));
		
		////////////////////////////////////////////////////////////
		// Create button section for add/remove/up/down buttons
		////////////////////////////////////////////////////////////
		buttonsComposite = toolkit.createComposite(tableAndButtonsComposite);
		buttonsComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		buttonsComposite.setLayout(new FillLayout(SWT.VERTICAL));

		////////////////////////////////////////////////////////////
		// Create table
		// allow table to fill entire width if there are no buttons
		////////////////////////////////////////////////////////////
		int span = 2;
		if ((style & CUSTOM_BUTTONS_MASK)!=0) {
			span = 1;
		}
		else {
			buttonsComposite.setVisible(false);
		}
		table = toolkit.createTable(tableAndButtonsComposite, SWT.FULL_SELECTION | SWT.V_SCROLL);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true, span, 1);
		gridData.widthHint = 100;
		gridData.heightHint = 100;
		table.setLayoutData(gridData);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		
		////////////////////////////////////////////////////////////
		// Create buttons for add/remove/up/down
		////////////////////////////////////////////////////////////
		if ((style & ADD_BUTTON)!=0) {
			addButton = toolkit.createButton(buttonsComposite, "Add", SWT.PUSH);
		}

		if ((style & REMOVE_BUTTON)!=0) {
			removeButton = toolkit.createButton(buttonsComposite, "Remove", SWT.PUSH);
			removeButton.setEnabled(false);
		}
		
		if ((style & MOVE_BUTTONS)!=0) {
			upButton = toolkit.createButton(buttonsComposite, "Up", SWT.PUSH);
			upButton.setEnabled(false);
	
			downButton = toolkit.createButton(buttonsComposite, "Down", SWT.PUSH);
			downButton.setEnabled(false);
		}
		
		if ((style & EDIT_BUTTON)!=0) {
			editButton = toolkit.createButton(buttonsComposite, "Edit...", SWT.PUSH);
			editButton.setEnabled(false);
		}

		
	}
	
	public class ContentProvider implements IStructuredContentProvider {
		private EObject object;
		private EStructuralFeature feature;
		private EList<EObject> list;
		
		public ContentProvider(EObject object, EStructuralFeature feature, EList<EObject> list) {
			this.object = object;
			this.feature = feature;
			this.list = list;
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (listItemClass==null) {
				// display all items in the list that are subclasses of listItemClass
				return list.toArray();
			}
			else {
				// we're only interested in display specific EClass instances
				List<EObject> elements = new ArrayList<EObject>();
				for (EObject o : list) {
					EClass ec = o.eClass();
					EClass lic = getListItemClass(object,feature);
					if (ec == lic)
						elements.add(o);
				}
				return elements.toArray(new EObject[elements.size()]);
			}
		}
	}
	
	public class TableColumn extends ColumnTableProvider.Column implements ILabelProvider, ICellModifier {
		private TableViewer tableViewer;
		private EStructuralFeature feature;
		private EObject object;
		
		public TableColumn(EObject o, EAttribute a) {
			object = o;
			feature = a;
		}
		
		public void setTableViewer(TableViewer t) {
			tableViewer = t;
		}
		
		@Override
		public String getHeaderText() {
			return ModelUtil.toDisplayName(feature.getName());
		}

		@Override
		public String getProperty() {
			if (feature!=null)
				return feature.getName(); //$NON-NLS-1$
			return "";
		}

		@Override
		public int getInitialWeight() {
			return 10;
		}

		public String getText(Object element) {
			Object value = ((EObject)element).eGet(feature);
			return value==null ? "" : value.toString();
		}
		
		public CellEditor createCellEditor (Composite parent) {
			if (feature!=null) {
				EClassifier ec = feature.getEType();
				if (ec instanceof EDataType) {
					return new EDataTypeCellEditor((EDataType)ec, parent);
				}
			}
			return null;
		}
		
		public boolean canModify(Object element, String property) {
			return tableProvider.canModify(object, feature, (EObject)element);
		}

		public void modify(Object element, String property, Object value) {
			final EObject target = (EObject)element;
			final Object newValue = value;
			final Object oldValue = target.eGet(feature); 
			if (oldValue==null || !oldValue.equals(value)) {
				TransactionalEditingDomain editingDomain = bpmn2Editor.getEditingDomain();
				editingDomain.getCommandStack().execute(new RecordingCommand(editingDomain) {
					@Override
					protected void doExecute() {
						target.eSet(feature, newValue);
					}
				});
				if (bpmn2Editor.getDiagnostics()!=null) {
					// revert the change and display error status message.
					bpmn2Editor.showErrorMessage(bpmn2Editor.getDiagnostics().getMessage());
				}
				else
					bpmn2Editor.showErrorMessage(null);
				tableViewer.refresh();
			}
		}

		@Override
		public Object getValue(Object element, String property) {
			return getText(element);
		}
	}

	public abstract class AbstractTableProvider extends ColumnTableProvider {
		
		/**
		 * Implement this to select which columns are editable
		 * @param object - the list object
		 * @param feature - the feature of the item contained in the list
		 * @param item - the selected item in the list
		 * @return true to allow editing
		 */
		public abstract boolean canModify(EObject object, EStructuralFeature feature, EObject item);
	}
}