package pku.abe.commons.switcher;

import java.util.Collection;

public interface SwitcherManager {

    /**
     * 注册开关 </br>
     * 如果开关不存在，则自动创建，并赋默认值。 </br>
     * 如果开关存在，则关联 switcherChangeListener 到该开关。如果该开关的当前值和默认值不一样，则先触发一次
     * {@link SwitcherChangeListener#onValueChange(Switcher, boolean)} </br>
     *
     * @param switcherName
     * @param switcherChangeListener
     * @param defaultValue
     * @return 已经存在的同名开关或者新创建的开关 不会为null
     */
    public abstract Switcher registerSwitcher(String switcherName, SwitcherChangeListener switcherChangeListener, boolean defaultValue);

    /**
     * @param switcherName
     * @param defaultValue
     * @return
     * @see #registerSwitcher(String, SwitcherChangeListener, boolean)
     */
    public abstract Switcher registerSwitcher(String switcherName, boolean defaultValue);

    /**
     * 将 switcherChangeListener 关联到该开关，如果该开关不存在则返回 false
     *
     * @param switcherName
     * @param switcherChangeListener
     * @return
     */
    public abstract boolean watch(String switcherName, SwitcherChangeListener switcherChangeListener);

    /**
     * 通过 switcherName 获取 Switcher,如果不存在则返回null.
     *
     * @param switcherName
     * @return
     */
    public abstract Switcher getSwitcher(String switcherName);

    /**
     * 设置switcher的值，如果switcher不存在，则抛异常 {@link SwitcherNotFindException}
     *
     * @param switcherName
     * @param newValue
     * @return 当前switcher
     */
    public abstract Switcher setSwitcher(String switcherName, boolean newValue);

    public abstract Collection<Switcher> listSwitchers();

    /**
     * 清空开关
     */
    public void clearAll();

}
