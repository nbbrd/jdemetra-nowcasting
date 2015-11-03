/*
 * Copyright 2014 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package be.nbb.demetra.dfm.output.news;

import static be.nbb.demetra.dfm.output.news.NewsImpactsView.ALL_NEWS;
import static be.nbb.demetra.dfm.output.news.NewsImpactsView.ALL_REVISIONS;
import be.nbb.demetra.dfm.output.news.outline.CustomNode;
import be.nbb.demetra.dfm.output.news.outline.VariableNode;
import be.nbb.demetra.dfm.output.news.outline.XOutline.Title;
import ec.tss.dfm.DfmResults;
import ec.tss.dfm.DfmSeriesDescriptor;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.dfm.DfmNews;
import ec.tstoolkit.timeseries.information.TsInformationSet;
import ec.tstoolkit.timeseries.information.TsInformationUpdates;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Mats Maggi
 */
public final class NewsImpactsDataExtractor {

    private final DfmNews doc;
    private final DfmResults results;

    private List<Title> titles;
    private List<TsPeriod> newPeriods;
    private List<TsPeriod> oldPeriods;
    private List<Double> all_news;
    private List<Double> all_revisions;
    private List<Double> old_forecasts;
    private List<Double> new_forecasts;
    private List<DataBlock> news_impacts;
    private List<DataBlock> revisions_impacts;
    private Map<TsPeriod, Double> old_forecasts2;
    private Map<TsPeriod, Double> new_forecasts2;
    private Map<TsPeriod, Double> all_revisions2;
    private Map<TsPeriod, Double> all_news2;
    private Map<String, Integer> indexOfSeries;
    private int idx;

    private List<CustomNode> nodes = new ArrayList<>();

    public NewsImpactsDataExtractor(DfmNews doc, DfmResults results) {
        this.doc = doc;
        this.results = results;
    }

    public void calculateData(int selected) {
        DfmSeriesDescriptor[] desc = results.getDescriptions();
        DataBlock n = doc.news();
        DataBlock r = doc.revisions();
        TsInformationSet dataNew = doc.getNewInformationSet();
        TsInformationSet dataOld = doc.getOldInformationSet();
        TsData sNew = dataNew.series(selected);
        TsData sOld = dataOld.series(selected);
        TsFrequency freq = doc.getNewsDomain().getFrequency();
        TsPeriod newsStart = null, revsStart = null;

        newPeriods = new ArrayList<>();
        oldPeriods = new ArrayList<>();
        all_news = new ArrayList<>();
        all_revisions = new ArrayList<>();
        old_forecasts = new ArrayList<>();
        new_forecasts = new ArrayList<>();
        news_impacts = new ArrayList<>();
        revisions_impacts = new ArrayList<>();
        old_forecasts2 = new HashMap<>();
        new_forecasts2 = new HashMap<>();
        all_revisions2 = new HashMap<>();
        all_news2 = new HashMap<>();
        indexOfSeries = new HashMap<>();
        idx = 0;

        double mean = desc[selected].mean;
        double stdev = desc[selected].stdev;

        if (!doc.newsDetails().news().isEmpty()) {
            newsStart = doc.getNewsDomain().getStart();
        }
        if (!doc.newsDetails().revisions().isEmpty()) {
            revsStart = doc.getRevisionsDomain().getStart();
        }

        for (int j = sNew.getLength() - 1; j >= 0; --j) {
            if (sNew.isMissing(j)) {
                TsPeriod p = sNew.getDomain().get(j);
                TsPeriod pN = p.lastPeriod(freq);
                if (pN.isNotBefore(doc.getNewsDomain().getStart())) {
                    newPeriods.add(p);

                    DataBlock news_weights = doc.weights(selected, pN); // Get weights
                    all_news.add(n.dot(news_weights) * stdev);
                    all_news2.put(p, n.dot(news_weights) * stdev);

                    double newValue = (doc.getNewForecast(selected, pN) * stdev) + mean;
                    new_forecasts.add(newValue);
                    new_forecasts2.put(p, newValue);

                    news_impacts.add(new DataBlock(news_weights.getLength()));
                    for (int k = 0; k < news_weights.getLength(); k++) {
                        news_impacts.get(news_impacts.size() - 1).set(k, n.get(k) * news_weights.get(k) * stdev);
                    }

                    if (!doc.newsDetails().revisions().isEmpty()) {
                        DataBlock revisions_weights = doc.weightsRevisions(selected, pN); // Get weights
                        all_revisions.add(r.dot(revisions_weights) * stdev);
                        all_revisions2.put(p, r.dot(revisions_weights) * stdev);

                        revisions_impacts.add(new DataBlock(revisions_weights.getLength()));
                        for (int k = 0; k < revisions_weights.getLength(); k++) {
                            revisions_impacts.get(revisions_impacts.size() - 1).set(k, r.get(k) * revisions_weights.get(k) * stdev);
                        }
                    }
                }
            } else {
                break;
            }
        }

        for (int j = sOld.getLength() - 1; j >= 0; --j) {
            if (sOld.isMissing(j)) {
                TsPeriod p = sOld.getDomain().get(j);
                TsPeriod pO = p.lastPeriod(freq);
                if (pO.isNotBefore(doc.getNewsDomain().getStart())) {
                    oldPeriods.add(p);
                    double oldValue = (doc.getOldForecast(selected, pO) * stdev) + mean;
                    old_forecasts.add(oldValue);
                    old_forecasts2.put(p, oldValue);
                }
            } else {
                break;
            }
        }

        Collections.reverse(newPeriods);
        Collections.reverse(oldPeriods);
        Collections.reverse(all_news);
        Collections.reverse(all_revisions);
        Collections.reverse(old_forecasts);
        Collections.reverse(new_forecasts);
        Collections.reverse(news_impacts);
        Collections.reverse(revisions_impacts);

        createColumnTitles();

        //================================================
        TsInformationUpdates details = doc.newsDetails();
        List<TsInformationUpdates.Update> updates = details.news();

        nodes = new ArrayList<>();

        VariableNode allNewsNode = new VariableNode(ALL_NEWS, null, null, null, all_news2);
        allNewsNode.setFullName(ALL_NEWS);
        indexOfSeries.put(allNewsNode.getFullName(), idx++);

        List<CustomNode> newsNodes = new ArrayList<>();
        for (int i = 0; i < updates.size(); i++) {
            TsInformationUpdates.Update updt = updates.get(i);
            TsPeriod p = updt.period;
            if (p.lastPeriod(freq).isNotBefore(newsStart)) {
                DfmSeriesDescriptor descriptor = desc[updt.series];
                String name = descriptor.description;

                Double exp = updt.getForecast() * descriptor.stdev + descriptor.mean;
                Double obs = updt.getObservation() * descriptor.stdev + descriptor.mean;

                Map<TsPeriod, Double> values = new HashMap<>();
                for (int j = 0; j < news_impacts.size(); j++) {
                    values.put(newPeriods.get(j), news_impacts.get(j).get(i));
                }
                VariableNode vn = new VariableNode(name, p, exp, obs, values);
                vn.setFullName(name + " (N) [" + p.toString() + "]");
                newsNodes.add(vn);
                indexOfSeries.put(vn.getFullName(), idx++);
            }
        }

        allNewsNode.setChildren(newsNodes);
        nodes.add(allNewsNode);

        List<TsInformationUpdates.Update> revisions = details.revisions();
        List<CustomNode> revNodes = new ArrayList<>();
        for (int i = 0; i < revisions.size(); i++) {
            TsPeriod p = revisions.get(i).period;
            if (p.lastPeriod(freq).isNotBefore(revsStart)) {
                DfmSeriesDescriptor descriptor = desc[revisions.get(i).series];
                String name = descriptor.description;

                Double exp = revisions.get(i).getForecast() * descriptor.stdev + descriptor.mean;
                Double obs = revisions.get(i).getObservation() * descriptor.stdev + descriptor.mean;

                Map<TsPeriod, Double> values = new HashMap<>();
                for (int j = 0; j < revisions_impacts.size(); j++) {
                    values.put(newPeriods.get(j), revisions_impacts.get(j).get(i));
                }
                VariableNode vn = new VariableNode(name, p, exp, obs, values);
                vn.setFullName(name + " (R) [" + p.toString() + "]");
                revNodes.add(vn);
            }
        }

        if (!revNodes.isEmpty()) {
            VariableNode allRevisionsNode = new VariableNode(ALL_REVISIONS, null, null, null, all_revisions2);
            allRevisionsNode.setFullName(ALL_REVISIONS);
            indexOfSeries.put(ALL_REVISIONS, idx++);
            allRevisionsNode.setChildren(revNodes);
            nodes.add(allRevisionsNode);

            for (CustomNode revNode : revNodes) {
                indexOfSeries.put(revNode.getFullName(), idx++);
            }
        }

        nodes.add(new VariableNode("Old Forecasts", null, null, null, old_forecasts2));
        indexOfSeries.put("Old Forecasts", -1);
        nodes.add(new VariableNode("New Forecasts", null, null, null, new_forecasts2));
        indexOfSeries.put("New Forecasts", -1);
    }

    private void createColumnTitles() {
        titles = new ArrayList<>();
        titles.add(new Title("Reference Period", "<html><center>Reference<br>Period"));
        titles.add(new Title("Expected Value", "<html><center>Expected<br>Value"));
        titles.add(new Title("Observed Value", "<html><center>Observed<br>Value"));
        for (TsPeriod p : newPeriods) {
            titles.add(new Title("Impact " + p.toString(), "<html><center>Impact<br>" + p.toString()));
        }
    }

    public List<Title> getTitles() {
        return titles;
    }

    public List<Double> getAllNews() {
        return all_news;
    }

    public List<TsPeriod> getNewPeriods() {
        return newPeriods;
    }

    public List<DataBlock> getNewsImpacts() {
        return news_impacts;
    }

    public List<Double> getAllRevisions() {
        return all_revisions;
    }

    public List<DataBlock> getRevisionsImpacts() {
        return revisions_impacts;
    }

    public List<CustomNode> getNodes() {
        return nodes;
    }

    public Map<String, Integer> getIndexOfSeries() {
        return indexOfSeries;
    }
}
