File request = new File(basedir, "target/test-1/request.xml")
File response = new File(basedir, "target/test-1/response.xml")
File weather = new File(basedir, "target/weather.txt")

assert request.text == '''<?xml version="1.0" encoding="UTF-8"?><GetWeather xmlns="http://www.webserviceX.NET"><CityName>Berlin-Tegel</CityName><CountryName>Germany</CountryName></GetWeather>'''
assert response.text == '''<?xml version="1.0" encoding="UTF-8" standalone="no"?><GetWeatherResponse xmlns="http://www.webserviceX.NET"><GetWeatherResult>Data Not Found</GetWeatherResult></GetWeatherResponse>'''
assert weather.text == 'Data Not Found'
