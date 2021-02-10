package sci.category.geovisit.supplier

import groovy.stream.Stream
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import sci.category.geovisit.constant.FactoryKey
import sci.category.geovisit.constant.OrgAddressKey
import spock.lang.Specification

@MicronautTest(packages = "sci.category.geovisit")
class OrgRelationshipSupplierSpec extends Specification{
    final static DELIMIT = "[|]"

    def "sanityCheckListAssembler"(){
        given:
        List<Map> root = [[root: "i am root"]]
        List<Map> statesList = [[state: "KS"]]
        List<Map> countyList = [[county: "Luthor"]]
        List<Map> citiesList = [[city: "Smallville"]]
        def bootstrap = Stream.from( root, statesList,countyList,citiesList ).inject([]){ all, l -> all += l }
        expect:
        bootstrap
        bootstrap.size() == 4
    }
    def "test explicit TDD build"() {
        given:
        def parseFileToMaps = new File("src/main/resources/citiesUsa.csv").readLines().inject([], {
            list, line ->
                List tokens = line.split(DELIMIT)
                Map map = [state: tokens[1], county: tokens[3], city: tokens[0]]
                list + map
        })
        def statesList = parseFileToMaps.groupBy([{ m -> m.state }, { m -> m.county }])
        def root = [state: "State short", county: "County"]
        def citiesList = []
        def stateCountyMap = [:]
        def foo = statesList.each {
            stateKey, Map countiesMap ->
                countiesMap.each {
                    countyKey, List<Map> cityMapList ->
                        cityMapList.each {
                            city ->
                                citiesList += city
                                def key = "${stateKey}|${countyKey}"
                                def parent = stateCountyMap.get(key)
                                parent = parent ?: [state: stateKey, county: countyKey]
                                stateCountyMap.put(key, parent)
                                city[(OrgAddressKey.Parent)] = parent
                                city[(OrgAddressKey.Child)] = city
                        }
                }
        }
        expect:
        parseFileToMaps
        statesList
        statesList == foo
    }

    def "test static newInstance"() {
        given:
        def root = [:]
        List<Map> build = [[(OrgAddressKey.Root): root]]
        Map config = [(OrgAddressKey.Root): root, (FactoryKey.Bootstrap): build]
        def builder = OrgRelationshipSupplier.Builder.newInstance(config)
        assert builder
        def supplier = builder.build()
        expect:
        supplier
        def supply = supplier.get()
        def rootVerify = supplier.getRoot()
        true
    }
}
