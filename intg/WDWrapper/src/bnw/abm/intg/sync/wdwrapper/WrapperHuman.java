package bnw.abm.intg.sync.wdwrapper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunState;
import WeddingGrid.Human;
import WeddingGrid.Female;
import WeddingGrid.Male;

public class WrapperHuman {

	private Human human = null;
	private Number globalId = null;
	private WrapperHuman mother = null;
	private WrapperHuman father = null;
	private WrapperHuman partner = null;
	private HashSet<WrapperHuman> children = new HashSet<WrapperHuman>();
	private boolean dead = false;
	private int yearOfDeath = 0;
	private double lastTick = -1;
	private boolean isYAxisAge = false;
	private int parity;
	private boolean isIll = false;

	public WrapperHuman(Human h, WrapperHuman mother, WrapperHuman father) {
		this.human = h;
		this.mother = mother;
		this.father = father;
		this.isYAxisAge = ((int) RunEnvironment.getInstance().getParameters().getValue("numOfTick")) > 0 ? false
				: true;
		int yearOfDeath = 0;
		try {
			Field fld1 = h.getClass().getSuperclass().getDeclaredField("yearOfDeath");
			fld1.setAccessible(true);
			yearOfDeath = (int) fld1.get(h); 
			Field fld2 = h.getClass().getSuperclass().getDeclaredField("isIll");
			fld2.setAccessible(true);
			this.isIll = (boolean) fld2.get(h);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
				| IllegalAccessException e) {
			e.printStackTrace();
		}
		this.setDead(yearOfDeath == 0?false:true, yearOfDeath);
	}

	public Human getHuman() {
		return this.human;
	}

	/**
	 * @return agent's local ID
	 */
	public Number getLocalID() {
		return this.human.getID();
	}

	/**
	 * @return the globalId
	 */
	public Number getGlobalID() {
		return globalId;
	}

	/**
	 * @param globalId
	 *            the globalId to set
	 */
	public void setGlobalID(Integer globalId) {
		this.globalId = globalId;
	}

	/**
	 * @return the father
	 */
	public WrapperHuman getFather() {
		return father;
	}

	public void setFather(WrapperHuman father) {
		this.father = father;
	}

	public void setMother(WrapperHuman mother) {
		this.mother = mother;
	}

	/**
	 * @return the mother
	 */
	public WrapperHuman getMother() {
		return this.mother;
	}

	public WrapperHuman getMotherAfterChecking() {
		Context<Object> context = RunState.getInstance().getMasterContext();
		if (lastTick == RunEnvironment.getInstance().getCurrentSchedule().getTickCount()) {
			return this.mother;
		}
		lastTick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		mother = null;
		for (Object ob : context) {
			if (ob instanceof Female) {
				Female mom = (Female) ob;
				ArrayList<Human> childrenLst = null;
				try {
					Field fld = mom.getClass().getSuperclass().getDeclaredField("children");
					fld.setAccessible(true);
					childrenLst = (ArrayList<Human>) fld.get(mom); // (ArrayList<Human>)
																	// Human.class.getField("children").get(mom);
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}

				for (Human child : childrenLst) {
					if (child.getID() == this.human.getID()) {
						mother = new SimulationState(RunState.getInstance().getMasterContext()).getWrapperHuman(context, mom);// This is ugly - recursive call
					}
				}
			}
		}
		return mother;

	}

	/**
	 * @return the dead
	 */
	public boolean isDead() {
		return dead;
	}

	public int getYearOfdeath() {
		return this.yearOfDeath;
	}

	/**
	 * Sets agent dead. Does not update model instance
	 * 
	 * @param dead
	 *            set agent dead and year of death
	 * @param year
	 *            year of death
	 */
	public void setDead(boolean dead, int year) {
		this.dead = dead;
		this.yearOfDeath = year;
	}

	public boolean isYAxisAge() {
		return isYAxisAge;
	}

	public void setChanged() {
		Class<?> c;
		Method method;
		try {
			c = Class.forName("WeddingGrid.Human");
			method = c.getSuperclass().getDeclaredMethod("setChanged");
			method.setAccessible(true);
			method.invoke(this.getHuman());
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void notifyObservers(){
		Class<?> c;
		Method method;
		try {
			c = Class.forName("WeddingGrid.Human");
			method = c.getSuperclass().getDeclaredMethod("notifyObservers");
			method.setAccessible(true);
			method.invoke(this.getHuman());
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isIll() {
		return isIll;
	}

	public void setIll(boolean isIll) {
		this.isIll = isIll;
	}

	/**
	 * @return the partner
	 */
	public WrapperHuman getPartner() {
		return partner;
	}

	/**
	 * @param partner the partner to set
	 */
	public void setPartner(WrapperHuman partner) {
		this.partner = partner;
	}

	/**
	 * @return the children
	 */
	public HashSet<WrapperHuman> getChildren() {
		return children;
	}

	/**
	 * @param children the children to set
	 */
	public void addChild(WrapperHuman child) {
		this.children.add(child);
	}
}