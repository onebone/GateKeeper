package me.onebone.gatekeeper;

/*
 * GateKeeper: A typical user login system for Nukkit
 * Copyright (C) 2016 onebone <jyc00410@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Comparator;

public class PermissionComparator implements Comparator<String>{

	@Override
	public int compare(String a, String b) {
		if(isChild(a, b)){
			return 1;
		}else if(isChild(b, a)){
			return -1;
		}
		return 0;
	}
	
	public static boolean isChild(String perm, String target){
		String[] permArr = perm.split("\\.");
		String[] targetArr = target.split("\\.");
		
		if(permArr.length > targetArr.length) return false; 
		for(int i = 0; i < permArr.length; i++){
			if(!permArr[i].equals(targetArr[i])) return false;
		}
		return true;
	}
}
