package com.epam.jdi.uitests.core.initialization;

/**
 * Created by Roman Iovlev on 14.02.2018
 * Email: roman.iovlev.jdi@gmail.com; Skype: roman.iovlev
 */

import com.epam.jdi.uitests.core.annotations.Root;
import com.epam.jdi.uitests.core.interfaces.ISetup;
import com.epam.jdi.uitests.core.interfaces.base.IBaseElement;
import com.epam.jdi.uitests.core.interfaces.base.IComposite;
import com.epam.jdi.uitests.core.interfaces.complex.IList;
import com.epam.jdi.uitests.core.interfaces.complex.tables.IEntityTable;
import com.epam.jdi.uitests.core.interfaces.composite.IPage;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static com.epam.jdi.tools.LinqUtils.foreach;
import static com.epam.jdi.tools.ReflectionUtils.*;
import static com.epam.jdi.tools.StringUtils.LINE_BREAK;
import static com.epam.jdi.tools.TryCatchUtil.tryGetResult;
import static com.epam.jdi.tools.switcher.SwitchActions.Else;
import static com.epam.jdi.tools.switcher.SwitchActions.Switch;
import static com.epam.jdi.uitests.core.initialization.MapInterfaceToElement.getClassFromInterface;
import static com.epam.jdi.uitests.core.settings.JDISettings.exception;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.asList;

public abstract class CascadeInit {
    protected Class<?>[] decorators() {
        return new Class<?>[]{IBaseElement.class, List.class};
    }

    /**
     * Init non static fields
     *
     * @param parent
     * @param driverName
     */
    public synchronized void initElements(Object parent, String driverName) {
        setFieldsForInit(parent, getFields(parent, decorators(), stopTypes()), parent.getClass(), driverName);
    }

    protected abstract Class<?>[] stopTypes();

    protected abstract IBaseElement fillInstance(IBaseElement instance, Field field);

    protected abstract <T> T getNewLocatorFromField(Field field);

    protected abstract void fillPageFromAnnotation(Field field, IBaseElement instance, Class<?> parentType);

    /**
     * Get instance page
     *
     * @param parentType
     * @param driverName
     */
    public synchronized void initStaticPages(Class<?> parentType, String driverName) {
        setFieldsForInit(null,
                getFields(asList(parentType.getDeclaredFields()), decorators(), f -> isStatic(f.getModifiers())),
                parentType, driverName);
    }

    /**
     * Set fields for init
     *
     * @param parent
     * @param fields
     * @param parentType
     * @param driverName
     */
    private void setFieldsForInit(Object parent, List<Field> fields, Class<?> parentType, String driverName) {
        foreach(fields, field -> setElement(parent, parentType, field, driverName));
    }

    /**
     * Init site as instance
     *
     * @param site
     * @param driverName
     */
    public synchronized <T> T initPages(Class<T> site, String driverName) {
        T instance = tryGetResult(site::newInstance);
        initElements(instance, driverName);
        return instance;
    }

    /**
     * Set field for init
     * @param parent
     * @param parentType
     * @param field
     * @param driverName
     */
    private void setElement(Object parent, Class<?> parentType, Field field, String driverName) {
        try {
            Class<?> type = field.getType();
            IBaseElement instance = isInterface(type, IPage.class)
                    ? getInstancePage(parent, field, type, parentType)
                    : getInstanceElement(parent, type, parentType, field, driverName);
            instance.setName(field);
            if (parent != null)
                instance.setDriverName(driverName);
            instance.setTypeName(type.getSimpleName());
            field.set(parent, instance);
            if (isInterface(field, IComposite.class))
                initElements(instance, driverName);
        } catch (Exception ex) {
            throw exception("Error in setElement for field '%s' with parent '%s'", field.getName(),
                    parentType == null ? "NULL Class" : parentType.getSimpleName() + LINE_BREAK + ex.getMessage());
        }
    }

    /**
     * Get instance page
     * @param parent
     * @param field
     * @param type
     * @param parentType
     * @return IBaseElement
     */
    private IBaseElement getInstancePage(Object parent, Field field, Class<?> type, Class<?> parentType) throws IllegalAccessException, InstantiationException {
        IBaseElement instance = (IBaseElement) getValueField(field, parent);
        if (instance == null)
            instance = (IBaseElement) type.newInstance();
        fillPageFromAnnotation(field, instance, parentType);
        return instance;
    }

    /**
     * Get instance element
     * @param parent
     * @param type
     * @param parentType
     * @param field
     * @param driverName
     * @return IBaseElement
     */
    private IBaseElement getInstanceElement(Object parent, Class<?> type, Class<?> parentType, Field field, String driverName) {
        return createChildFromFieldStatic(parent, parentType, field, type, driverName);
    }

    /**
     * Apply specific action
     * @param instance
     * @param field
     * @param parent
     * @param type
     * @return IBaseElement
     */
    protected IBaseElement specificAction(IBaseElement instance, Field field, Object parent, Class<?> type) {
        return instance;
    }

    /**
     * Fill from JDI annotation
     * @param instance
     * @param field
     * @return IBaseElement
     */
    protected IBaseElement fillFromJDIAnnotation(IBaseElement instance, Field field) {
        return instance;
    }

    /**
     * Create child from static field
     * @param parent
     * @param parentClass
     * @param field
     * @param type
     * @param driverName
     * @return IBaseElement
     */
    private IBaseElement createChildFromFieldStatic(Object parent, Class<?> parentClass, Field field, Class<?> type, String driverName) {
        IBaseElement instance = (IBaseElement) getValueField(field, parent);
        try {
            instance = instance == null
                    ? getElementInstance(field, driverName, parent)
                    : fillInstance(instance, field);
        } catch (Exception ex) {
            throw exception(format("Can't create child for parent '%s' with type '%s'. Exception: %s",
                    parentClass.getSimpleName(), field.getType().getSimpleName(),
                    ex.getMessage()));
        }
        instance.setParent(field.isAnnotationPresent(Root.class)
                ? null
                : parent);
        instance = fillFromJDIAnnotation(instance, field);
        instance = specificAction(instance, field, parent, type);
        return instance;
    }

    /**
     * Get element instance
     * @param field
     * @param driverName
     * @param parent
     * @return IBaseElement
     */
    private IBaseElement getElementInstance(Field field, String driverName, Object parent) {
        Class<?> type = field.getType();
        try {
            return getElementsRules(field, driverName, type);
        } catch (Exception ex) {
            throw exception("Error in getElementInstance for field '%s'%s with type '%s'",
                    field.getName(),
                    parent != null ? "in " + parent.getClass().getSimpleName() : "",
                    type.getSimpleName() + LINE_BREAK + ex.getMessage());
        }
    }

    /**
     * Get new locator
     * @param field
     */
    protected <T> T getNewLocator(Field field) {
        try {
            return getNewLocatorFromField(field);
        } catch (Exception ex) {
            throw exception("Error in get locator for type '%s'", field.getType().getName()
                    + LINE_BREAK + ex.getMessage());
        }
    }

    /**
     * Get element rules
     * @param field
     * @param driverName
     * @param type
     * @return IBaseElement
     */
    protected IBaseElement getElementsRules(Field field, String driverName, Class<?> type) {
        IBaseElement instance = Switch(type).get(
            Case(t -> isInterface(type, IEntityTable.class),
                t -> initEntityTable(field)),
            Case(t -> isInterface(type, List.class),
                t -> initList(field)),
            Else(t -> initElement(t, field)));
        instance.engine().setDriverName(driverName);
        return instance;
    }

    /**
     * Init entity table
     * @param field
     * @return IBaseElement
     */
    private IBaseElement initEntityTable(Field field) {
        Type[] types = ((ParameterizedType) field.getGenericType())
                .getActualTypeArguments();
        try {
            return (IBaseElement) getClassFromInterface(IEntityTable.class, field.getName())
                    .getDeclaredConstructor(Class.class, Class.class).newInstance(types[0], types[1]);
        } catch (Exception ex) {
            throw exception("Can't init EntityTable for %s. Exception: %s", field.getName(), ex.getMessage());
        }
    }

    /**
     * Init element
     * @param type
     * @param field
     * @return IBaseElement
     */
    private IBaseElement initElement(Class<?> type, Field field) {
        Class<?> elType = type.isInterface() ? getClassFromInterface(type, field.getName()) : type;
        try {
            return (IBaseElement) elType.newInstance();
        } catch (Exception ex) {
            throw exception("Can't init common element for %s. Exception: %s", field.getName(), ex.getMessage());
        }
    }

    /**
     * Init list
     * @param field
     * @return IBaseElement
     */
    private IBaseElement initList(Field field) {
        try {
            Class<?> elementClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            if (elementClass.isInterface())
                elementClass = getClassFromInterface(elementClass, field.getName());
            return (IBaseElement) getClassFromInterface(IList.class, field.getName())
                    .getDeclaredConstructor(Class.class).newInstance(elementClass);
        } catch (Exception ex) {
            throw exception("Can't init List element for %s. Exception: %s", field.getName(), ex.getMessage());
        }
    }

    /**
     * Fill from annotation
     * @param instance
     * @param field
     */
    protected static void fillFromAnnotation(IBaseElement instance, Field field) {
        try {
            ISetup setup = (ISetup) instance;
            setup.setup(field);
        } catch (Exception ignore) {
        }
    }
}