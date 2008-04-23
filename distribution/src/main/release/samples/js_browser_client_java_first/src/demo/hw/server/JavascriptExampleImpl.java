/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package demo.hw.server;

import java.util.ArrayList;
import java.util.List;

import javax.jws.WebService;

import demo.hw.server.data.Beverage;
import demo.hw.server.data.Category;
import demo.hw.server.data.Ingredient;


@WebService(endpointInterface = "demo.hw.server.JavascriptExample")
public class JavascriptExampleImpl implements JavascriptExample {
    
    private List<Beverage> beverages;
    
    public JavascriptExampleImpl() {
        beverages = new ArrayList<Beverage>();
        Ingredient chalk = new Ingredient();
        chalk.setName("chalk");
        chalk.setCategory(Category.Mineral);
        Ingredient broccoli = new Ingredient();
        broccoli.setName("broccoli");
        broccoli.setCategory(Category.Vegetable);
        Ingredient locust = new Ingredient();
        locust.setName("locust");
        locust.setCategory(Category.Animal);
        
        Beverage b = new Beverage();
        b.setName("broccoli shake");
        b.setProof(20);
        Ingredient[] ingredients;
        ingredients = new Ingredient[2];
        ingredients[0] = chalk;
        ingredients[1] = broccoli;
        b.setIngredients(ingredients);
        beverages.add(b);
        
        b = new Beverage();
        b.setName("green locust");
        b.setProof(50);
        ingredients = new Ingredient[2];
        ingredients[0] = locust;
        ingredients[1] = broccoli;
        b.setIngredients(ingredients);
        beverages.add(b);
        
        b = new Beverage();
        b.setName("bug shake");
        b.setProof(13);
        ingredients = new Ingredient[2];
        ingredients[0] = locust;
        ingredients[1] = chalk;
        b.setIngredients(ingredients);
        beverages.add(b);
        

    }

    public Beverage[] getBeverageWithIngredientCategories(List<Category> c) {
        List<Beverage> matches = new ArrayList<Beverage>();
        for (Beverage b : beverages) {
            for (Ingredient i : b.getIngredients()) {
                if (c.contains(i.getCategory())) {
                    matches.add(b);
                    break;
                }
            }
        }
        return matches.toArray(new Beverage[0]);
    }

    public Beverage[] getBeverageWithIngredientCategory(Category c) {
        List<Beverage> matches = new ArrayList<Beverage>();
        for (Beverage b : beverages) {
            for (Ingredient i : b.getIngredients()) {
                if (c == i.getCategory()) {
                    matches.add(b);
                    break;
                }
            }
        }
        return matches.toArray(new Beverage[0]);
    }

    public Beverage[] getBeveragesWithIngredient(Ingredient i) {
        List<Beverage> matches = new ArrayList<Beverage>();
        for (Beverage b : beverages) {
            for (Ingredient bi : b.getIngredients()) {
                if (bi == i) {
                    matches.add(b);
                    break;
                }
            }
        }
        return matches.toArray(new Beverage[0]);
    }
}
