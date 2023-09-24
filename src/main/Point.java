package main;

import java.awt.Color;

public class Point {
	
	Color color;
	int x = 0;
	int y = 0;

	public Point(int x, int y) {
		this.setX(x);
		this.setY(y);
	}

	public static void main(String[] args) {
		new Point(3,4);
	}

	public void setX(int x) {
		this.x = x;
		System.out.println("The x-value of point has been set to " + x);
	}
	
	public void setY(int y) {
		this.y = y;
		System.out.println("The y-value of point has been set to " + y);
	}

	public void setColor(Color c) {
		this.color = c;
		System.out.println("The color of point has been set.");
	}

}
